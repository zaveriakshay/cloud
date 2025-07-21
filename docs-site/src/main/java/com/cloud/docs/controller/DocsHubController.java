package com.cloud.docs.controller;

import com.cloud.docs.config.MarkdownConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/hub")
@RequiredArgsConstructor
@Slf4j
public class DocsHubController {

    private final DocsHubRegistry docsHubRegistry;
    private final ApiRegistryService apiRegistry;
    private final SpaceService spaceService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    @GetMapping("/{hubName}")
    public String getHub(
            @PathVariable String hubName,
            @RequestParam(name = "article", required = false) String articleId, // For deep linking
            Model model) {

        docsHubRegistry.getHub(hubName).ifPresentOrElse(hub -> {
            model.addAttribute("hub", hub);
            model.addAttribute("articleTree", hub.getArticleTree());

            Optional<DocsArticleNode> articleToShow = Optional.ofNullable(articleId)
                    .flatMap(hub::findArticleById)
                    .or(() -> hub.getFlatArticleList().stream().findFirst());

            if (articleToShow.isPresent()) {
                DocsArticleNode article = articleToShow.get();
                log.info("Initial article for hub '{}' is '{}'", hubName, article.id());
                model.addAttribute("firstArticle", article);

                String rawHtml = htmlRenderer.render(markdownParser.parse(article.content()));
                String transformedHtml = transformMarkdownLinks(rawHtml, hub, article.path());

                model.addAttribute("article", article);
                model.addAttribute("renderedContent", transformedHtml);
                model.addAttribute("navigation", hub.getNavigationMap().get(article.id()));
            } else {
                log.warn("Hub '{}' has no articles to display.", hubName);
                model.addAttribute("firstArticle", null);
                model.addAttribute("article", null);
                model.addAttribute("renderedContent", "");
                model.addAttribute("navigation", null);
            }

        }, () -> {
            model.addAttribute("error", "Documentation hub '" + hubName + "' not found.");
        });

        model.addAttribute("docsHubRegistry", docsHubRegistry);
        model.addAttribute("allApis", apiRegistry.getSpecifications());
        model.addAttribute("spaceService", spaceService);
        model.addAttribute("activeTab", hubName);

        return "docs-hub";
    }

    @GetMapping("/{hubName}/article/{*id}")
    public String getArticle(@PathVariable String hubName, @PathVariable String id, Model model) {
        Optional<DocsHubService> hubOptional = docsHubRegistry.getHub(hubName);

        if (hubOptional.isEmpty()) {
            log.warn("HTMX request for article failed: Hub '{}' not found.", hubName);
            model.addAttribute("error", "Documentation hub '" + hubName + "' not found.");
            return "fragments/hub-fragments :: error-content";
        }

        DocsHubService hub = hubOptional.get();
        model.addAttribute("hub", hub);

        String sanitizedId = id.startsWith("/") ? id.substring(1) : id;
        Optional<DocsArticleNode> articleOptional = hub.findArticleById(sanitizedId);

        if (articleOptional.isPresent()) {
            DocsArticleNode article = articleOptional.get();
            log.info("HTMX request successful: Found article '{}' in hub '{}'.", sanitizedId, hubName);
            String rawHtml = htmlRenderer.render(markdownParser.parse(article.content()));
            String transformedHtml = transformMarkdownLinks(rawHtml, hub, article.path());

            model.addAttribute("article", article);
            model.addAttribute("renderedContent", transformedHtml);
            model.addAttribute("navigation", hub.getNavigationMap().get(sanitizedId));
        } else {
            log.warn("HTMX request failed: Article with id '{}' not found in hub '{}'.", sanitizedId, hubName);
        }

        return "fragments/hub-fragments :: article-content";
    }

    @GetMapping("/{hubName}/toc/{*id}")
    public String getTableOfContents(@PathVariable String hubName, @PathVariable String id, Model model) {
        String sanitizedId = id.startsWith("/") ? id.substring(1) : id;

        List<Map<String, String>> toc = docsHubRegistry.getHub(hubName)
                .flatMap(hub -> hub.findArticleById(sanitizedId))
                .map(article -> generateTocFromContent(article.content()))
                .orElse(Collections.emptyList());

        model.addAttribute("toc", toc);
        return "fragments/hub-fragments :: table-of-contents";
    }

    /**
     * Generates a Table of Contents list from markdown content.
     */
    private List<Map<String, String>> generateTocFromContent(String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> toc = new ArrayList<>();
        Node document = markdownParser.parse(markdownContent);

        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                if (heading.getLevel() == 2 || heading.getLevel() == 3) {
                    // Use a visitor to collect all text content from this heading
                    StringBuilder sb = new StringBuilder();
                    AbstractVisitor textCollector = new AbstractVisitor() {
                        @Override
                        public void visit(Text text) {
                            sb.append(text.getLiteral());
                        }
                    };
                    heading.accept(textCollector);
                    String title = sb.toString();

                    if (!title.isEmpty()) {
                        String anchor = MarkdownConfig.slugify(title);
                        toc.add(Map.of(
                                "title", title,
                                "anchor", anchor,
                                "level", String.valueOf(heading.getLevel())
                        ));
                    }
                }
                // Continue visiting other nodes in the document
                super.visit(heading);
            }
        });
        return toc;
    }

    private String transformMarkdownLinks(String htmlContent, DocsHubService hub, String currentArticlePath) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parseBodyFragment(htmlContent);
        Elements links = doc.select("a[href]");

        Path parentPath = Paths.get(currentArticlePath).getParent();

        for (Element link : links) {
            String href = link.attr("href");
            if (href.endsWith(".md")) {
                Path targetPath = (parentPath != null ? parentPath : Paths.get("")).resolve(href).normalize();
                String targetPathString = targetPath.toString().replace("\\", "/");

                String articleId = targetPathString.replace(".md", "").replaceAll("/", "-");

                if (hub.findArticleById(articleId).isPresent()) {
                    link.attr("href", "#");
                    link.attr("hx-get", String.format("/hub/%s/article/%s", hub.getHubName(), articleId));
                    link.attr("hx-target", "#docs-article-content");
                    link.attr("hx-swap", "innerHTML");
                    link.attr("x-on:click.prevent", String.format("activeArticleId = '%s'", articleId));
                } else {
                    link.attr("href", "#broken-link");
                    link.addClass("text-red-500 line-through");
                    link.attr("title", "Broken link: " + href);
                }
            }
        }
        return doc.body().html();
    }
}