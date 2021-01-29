package ru.kbakaras.cop.adoc;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Column;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.DescriptionList;
import org.asciidoctor.ast.DescriptionListEntry;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.List;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.converter.ConverterFor;
import org.asciidoctor.converter.StringConverter;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConverterFor("confluence")
public class ConfluenceConverter extends StringConverter {

    private final String LINE_SEPARATOR = "\n";


    public ConfluenceConverter(String backend, Map<String, Object> opts) {
        super(backend, opts);
    }

    @Override
    public String convert(ContentNode node, String transform, Map<Object, Object> opts) {

        if (transform == null) {
            transform = node.getNodeName();
        }

        if (node instanceof Document) {
            Document document = (Document) node;
            return document.getContent().toString();

        } else if (node instanceof Section) {
            Section section = (Section) node;
            String level = Integer.toString(section.getLevel());

            return "<h" + level + ">" + section.getTitle() + "</h" + level + ">" + LINE_SEPARATOR + section.getContent();

        } else if (node instanceof PhraseNode) {
            PhraseNode phrase = (PhraseNode) node;

            switch (phrase.getType()) {
                case "emphasis":
                    return "<i>" + phrase.getText() + "</i>";
                case "strong":
                    return "<b>" + phrase.getText() + "</b>";
                case "monospaced":
                    return "<pre>" + phrase.getText() + "</pre>";
                case "link":
                    return "<a href='" + phrase.getTarget() + "'>" + phrase.getReftext() + "</a>";
                default:
                    return phrase.getText();
            }

        } else if (node instanceof Table) {
            Table table = (Table) node;

            StringBuilder tableBuilder = new StringBuilder();
            tableBuilder.append("<table class='wrapped relative-table'");
            Optional.ofNullable(table.getAttribute("width"))
                    .map(ConfluenceConverter::formatWidth)
                    .map(width -> String.format(" style='%s'", width))
                    .ifPresent(tableBuilder::append);
            tableBuilder.append(">");

            tableBuilder.append("<colgroup>");
            for (Column column : table.getColumns()) {
                tableBuilder.append("<col style='" + formatWidth(column.getWidth()) + "'></col>");
            }
            tableBuilder.append("</colgroup>");

            tableBuilder.append("<tbody>\n");

            table.getHeader().forEach(row -> {
                tableBuilder.append("<tr>\n");
                row.getCells().forEach(cell -> {
                    tableBuilder.append("<th>");
                    tableBuilder.append(cell.getText());
                    tableBuilder.append("</th>\n");
                });
                tableBuilder.append("</tr>\n");
            });

            table.getBody().forEach(row -> {
                tableBuilder.append("<tr>\n");
                row.getCells().forEach(cell -> {
                    String span = cell.getColspan() > 0 ? " colspan='3'" : "";
                    tableBuilder.append("<td" + span + ">");
                    tableBuilder.append(cell.getText());
                    tableBuilder.append("</td>\n");
                });
                tableBuilder.append("</tr>\n");
            });

            tableBuilder.append("\n</tbody>");
            tableBuilder.append("\n</table>\n");

            return tableBuilder.toString();

        } else if (transform.equals("paragraph")) {
            StructuralNode block = (StructuralNode) node;
            String content = block.getContent().toString();

            return "<p>" + content.replaceAll(LINE_SEPARATOR, " ") + "</p>\n";

        } else if (transform.equals("preamble")) {

            return ((StructuralNode) node).getContent().toString();

        } else if (transform.equals("image")) {
            Block block = (Block) node;

            StringBuilder imageBuilder = new StringBuilder();

            imageBuilder
                    .append("<p>")

                    .append("<ac:image")
                    .append(" ac:align='").append(block.getAttribute("align"))
                    .append("' ac:width='").append(block.getAttribute("width"))
                    .append("'>")

                    .append("<ri:attachment ri:filename='").append(block.getAttribute("target")).append("'/>")
                    .append("</ac:image>")
                    .append("</p>");

            return imageBuilder.toString();

        } else if (transform.equals("dlist")) {

            DescriptionList dlist = (DescriptionList) node;

            StringBuilder builder = new StringBuilder();
            for (DescriptionListEntry entry : dlist.getItems()) {
                builder.append("<p>")
                        .append("<strong>")
                        .append(entry.getTerms().get(0).getText())
                        .append("</strong>")
                        .append("</p>")
                        .append(entry.getDescription().convert());
            }
            return builder.toString();

        } else if (transform.equals("ulist")) {

            List list = (List) node;

            StringBuilder builder = new StringBuilder();
            builder.append("<ul>");
            for (StructuralNode listItem : list.getItems()) {
                builder.append("<li>");
                builder.append(listItem.convert());
                builder.append("</li>");
            }
            builder.append("</ul>");

            return builder.toString();

        } else if (transform.equals("list_item")) {

            ListItem item = (ListItem) node;
            return item.hasText() ? item.getText() : item.getContent().toString();

        }

        return null;
    }


    private static String formatWidth(int width) {
        return String.format("width: %d.0%%; ", width);
    }

    private static String formatWidth(Object width) {

        Matcher matcher = PATTERN_WIDTH.matcher(width.toString());
        if (matcher.matches()) {
            return formatWidth(Integer.parseInt(matcher.group(1)));
        } else {
            throw new IllegalArgumentException(String.format("Некорректное значение ширины: %s", width));
        }
    }

    private static final Pattern PATTERN_WIDTH = Pattern.compile("(\\d+)%?");

}
