package com.cloud.docs.config;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class MarkdownConfig {

    /**
     * Creates a shared, reusable Markdown parser instance.
     */
    @Bean
    public Parser markdownParser() {
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        return Parser.builder().extensions(extensions).build();
    }

    /**
     * Creates a shared, reusable HtmlRenderer instance.
     * This renderer is configured with a custom AttributeProvider that automatically
     * adds a URL-friendly 'id' attribute to every H2 and H3 heading it renders.
     * This is the key to making the "On this page" scroll-spy feature work.
     */
    @Bean
    public HtmlRenderer htmlRenderer() {
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );

        AttributeProviderFactory attributeProviderFactory = context -> new HeadingAttributeProvider();

        return HtmlRenderer.builder()
                .extensions(extensions)
                .attributeProviderFactory(attributeProviderFactory)
                .build();
    }

    /**
     * A helper method to convert a string into a URL-friendly slug.
     * E.g., "My Awesome Heading!" becomes "my-awesome-heading".
     */
    public static String slugify(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "") // remove special chars
                .trim()
                .replaceAll("\\s+", "-"); // replace spaces with hyphens
    }

    /**
     * Custom AttributeProvider to add 'id' attributes to Heading nodes.
     */
    static class HeadingAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof Heading) {
                // Use a visitor to collect all text content, even with nested markdown like bold/italic.
                StringBuilder sb = new StringBuilder();
                AbstractVisitor textVisitor = new AbstractVisitor() {
                    @Override
                    public void visit(Text text) {
                        sb.append(text.getLiteral());
                    }
                };
                node.accept(textVisitor);
                String headingText = sb.toString();

                if (!headingText.isEmpty()) {
                    attributes.put("id", slugify(headingText));
                }
            }
        }
    }
}
