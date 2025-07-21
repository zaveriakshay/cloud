package com.cloud.docs.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/space")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    // Endpoint to get the editor panel for a selected page
    @GetMapping("/editor/{pageId}")
    public String getEditor(@PathVariable String pageId, Model model) {
        PageNode page = spaceService.findNodeById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found"));
        model.addAttribute("page", page);
        return "fragments/space-fragments :: editor-panel";
    }

    /**
     * NEW: Endpoint to get just the info panel's content.
     */
    @GetMapping("/info/{pageId}")
    public String getInfoPanel(@PathVariable String pageId, Model model) {
        PageNode page = spaceService.findNodeById(pageId).orElse(null);
        model.addAttribute("page", page);
        return "fragments/space-fragments :: info-content";
    }

    /*
     * REMOVED: The /preview endpoint is no longer needed as we are using
     * the editor's built-in side-by-side preview functionality.
     */

    /**
     * FIX: Updated to use HX-Trigger to notify the client of a successful save.
     */
    @PostMapping("/page/{pageId}")
    public String savePage(@PathVariable String pageId,
                           @RequestParam String title,
                           @RequestParam String content,
                           Model model,
                           HttpServletResponse response) { // Inject HttpServletResponse
        PageNode updatedPage = spaceService.updatePage(pageId, title, content);
        model.addAttribute("page", updatedPage);

        // This response header tells HTMX to trigger a custom event on the body
        response.setHeader("HX-Trigger", "updateInfoPanel");

        // Add a status message to the model to be displayed in the editor footer
        model.addAttribute("saveStatus", "Saved at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return "fragments/space-fragments :: editor-panel";
    }

    // Endpoint to show the form for adding a new node (page or section)
    @GetMapping("/add-node-form")
    public String showAddNodeForm(@RequestParam String parentId, @RequestParam boolean isSection, Model model) {
        model.addAttribute("parentId", parentId);
        model.addAttribute("isSection", isSection);
        return "fragments/space-fragments :: add-node-form";
    }

    // Endpoint to handle the creation of a new node
    @PostMapping("/add-node")
    public String addNode(@RequestParam String parentId,
                          @RequestParam String title,
                          @RequestParam boolean isSection,
                          Model model) {
        PageNode newNode = spaceService.createNode(parentId, title, isSection);
        model.addAttribute("node", newNode);
        return "fragments/space-fragments :: tree-node"; // Return the HTML for the new node
    }

    // Endpoint to handle drag-and-drop reordering
    @PostMapping("/reorder")
    @ResponseBody // This endpoint doesn't need to return HTML
    public void reorderPages(@RequestParam String parentId, @RequestParam("pageIds[]") List<String> pageIds) {
        spaceService.reorderChildren(parentId, pageIds);
    }

    /**
     * NEW: Renames the workspace title.
     */
    @PostMapping("/rename")
    public String renameWorkspace(@RequestParam String title, Model model) {
        spaceService.renameWorkspace(title);
        // Return the updated header fragment for the left panel
        model.addAttribute("workspace", spaceService.getPageTree());
        return "fragments/space-fragments :: left-panel-header";
    }

    /**
     * NEW: Publishes the workspace and returns the updated main header.
     */
    @PostMapping("/publish")
    public String publishWorkspace(Model model) {
        spaceService.publishWorkspace();
        // We need allApis and activeTab to re-render the header correctly
        model.addAttribute("allApis", List.of()); // Assuming you might want to fetch this properly
        model.addAttribute("activeTab", "space");
        model.addAttribute("spaceService", spaceService);
        return "fragments :: header_tabs";
    }

}