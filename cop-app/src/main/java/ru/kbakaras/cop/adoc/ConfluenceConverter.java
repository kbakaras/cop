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
import ru.kbakaras.sugar.utils.StringUtils;
import ru.kbakaras.sugar.utils.UUIDComposer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ConverterFor("confluence")
public class ConfluenceConverter extends StringConverter {

    private final String LINE_SEPARATOR = "\n";
    private static final Pattern PATTERN_WIDTH = Pattern.compile("(\\d+)%?");


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

            return (document.hasAttribute("toc") ? toc(document.getTitle()) : "") + document.getContent();

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
                    return "<code>" + phrase.getText() + "</code>";
                case "link":
                    return "<a href='" + phrase.getTarget() + "'>" + phrase.getReftext() + "</a>";
                case "line":
                    return phrase.getText() + "<br/>";
                default:
                    return phrase.getText();
            }

        } else if (node instanceof Table) {
            Table table = (Table) node;

            StringBuilder tableBuilder = new StringBuilder();
            tableBuilder.append("<table class='wrapped relative-table'");

            Optional<Boolean> wide = Optional
                    .ofNullable((String) table.getAttribute("wide"))
                    .map(Boolean::parseBoolean)
                    .filter(value -> value);

            wide.ifPresent(value -> tableBuilder.append("data-layout='wide'"));

            Optional.ofNullable(table.getAttribute("width"))
                    .map(ConfluenceConverter::formatWidth)
                    .map(width -> String.format(" style='%s'", width))
                    .ifPresent(tableBuilder::append);
            tableBuilder.append(">");

            tableBuilder.append("<colgroup>");
            columnStyles(table.getColumns(), wide.map(value -> 960).orElse(680))
                    .forEach(style -> tableBuilder.append("<col style='").append(style).append("'></col>"));
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
                    String span = StringUtils.join(" ",
                            cell.getColspan() > 0 ? " colspan='" + cell.getColspan() + "'" : "",
                            cell.getRowspan() > 0 ? " rowspan='" + cell.getRowspan() + "'" : "");
                    tableBuilder.append("<td" + span + ">");
                    if ("asciidoc".equals(cell.getStyle())) {
                        tableBuilder.append(cell.getContent());
                    } else {
                        tableBuilder.append(cell.getText());
                    }
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
                    .append(" ac:align='").append(block.getAttribute("align")).append("'");

            Optional.ofNullable(block.getAttribute("width"))
                    .ifPresent(width -> imageBuilder.append(" ac:width='").append(width).append("'"));

            imageBuilder
                    .append(">")

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

        } else if (transform.equals("olist")) {

            List list = (List) node;

            StringBuilder builder = new StringBuilder();
            builder.append("<ol>");
            for (StructuralNode listItem : list.getItems()) {
                builder.append("<li>");
                builder.append(listItem.convert());
                builder.append("</li>");
            }
            builder.append("</ol>");

            return builder.toString();

        } else if (transform.equals("list_item")) {

            ListItem item = (ListItem) node;

            if (item.hasText()) {
                StringBuilder builder = new StringBuilder(item.getText());
                for (StructuralNode itemNode: item.getBlocks()) {
                    builder.append(itemNode.convert());
                }

                return builder.toString();

            } else {

                return item.getContent().toString();
            }

        } else if (transform.equals("listing")) {

            Block block = (Block) node;
            StringBuilder builder = new StringBuilder();

            builder.append(String.format(
                    "<ac:structured-macro ac:name='code' ac:schema-version='1' ac:macro-id='%s'>",
                    UUID.nameUUIDFromBytes(block.getSource().getBytes())));

            Optional.ofNullable((String) block.getAttribute("language"))
                    .map(String::toLowerCase)
                    .map(value -> String.format("<ac:parameter ac:name='language'>%s</ac:parameter>", value))
                    .ifPresent(builder::append);

            builder.append("<ac:plain-text-body>");
            builder.append("<![CDATA[").append(block.getSource()).append("]]>");
            builder.append("</ac:plain-text-body>");

            builder.append("</ac:structured-macro>");

            return builder.toString();
        }

        return null;
    }

    private Stream<String> columnStyles(Collection<Column> columns, int tableWidth) {

        int sum = columns.stream().mapToInt(Column::getWidth).sum();
        return columns
                .stream()
                .mapToInt(Column::getWidth)
                .map(width -> tableWidth * width / sum)
                .mapToObj(ConfluenceConverter::formatWidth);
    }

    private static String toc(String documentTitle) {

        UUIDComposer composer = new UUIDComposer(UUID.fromString("9d6d6c0b-bb40-488f-9f0b-bd4829ce1bc8"));
        UUID uuidExpand = UUID.nameUUIDFromBytes(documentTitle.getBytes());
        UUID uuidToc = composer.compose(uuidExpand);

        return  "<ac:structured-macro ac:name='expand' ac:schema-version='1' ac:macro-id='" + uuidExpand + "'>" +
                "<ac:parameter ac:name='title'>Оглавление</ac:parameter>" +
                "<ac:rich-text-body>" +
                "<ac:structured-macro ac:name='toc' ac:schema-version='1' ac:macro-id='" + uuidToc + "'>" +
                "<ac:parameter ac:name='printable'>false</ac:parameter>" +
                "</ac:structured-macro>" +
                "</ac:rich-text-body>" +
                "</ac:structured-macro>";
    }

    private static String formatWidth(int width) {
        return String.format("width: %d.0px; ", width);
    }

    private static String formatWidth(Object width) {

        Matcher matcher = PATTERN_WIDTH.matcher(width.toString());
        if (matcher.matches()) {
            return formatWidth(Integer.parseInt(matcher.group(1)));
        } else {
            throw new IllegalArgumentException(String.format("Некорректное значение ширины: %s", width));
        }
    }

}
