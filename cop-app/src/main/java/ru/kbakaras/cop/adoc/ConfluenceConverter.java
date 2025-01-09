package ru.kbakaras.cop.adoc;

import lombok.extern.slf4j.Slf4j;
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
import org.jruby.RubyArray;
import ru.kbakaras.sugar.utils.StringUtils;
import ru.kbakaras.sugar.utils.UUIDComposer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
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

        if (transform.equals("inline_anchor")) {
            String anchorId = node.getId();
            if (anchorId != null) {
                return anchor(anchorId);
            }
        }

        if (node instanceof Document) {
            Document document = (Document) node;

            StringBuilder result = new StringBuilder();

            Optional.ofNullable(document.getAttribute("disclaimer"))
                    .map(Object::toString)
                    .map(ConfluenceConverter::disclaimer)
                    .ifPresent(result::append);

            if (document.hasAttribute("toc")) {
                result.append(toc(document.getTitle()));
            }

            return result.append(document.getContent()).toString();

        } else if (node instanceof Section) {
            Section section = (Section) node;
            String level = Integer.toString(section.getLevel());

            StringBuilder result = new StringBuilder("<h" + level + ">");
            if (node.getId() != null) {
                result.append(anchor(node.getId()));
            }

            return result
                    .append(formatSectionTitle(section))
                    .append("</h").append(level).append(">")
                    .append(LINE_SEPARATOR)
                    .append(section.getContent())
                    .toString();

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
                case "xref":
                    String refText = Optional.ofNullable(phrase.getReftext()).orElse(phrase.getTarget());
                    return "<a href='" + phrase.getTarget() + "'>" + refText + "</a>";
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
                    String style = cell.getHorizontalAlignment() != Table.HorizontalAlignment.LEFT
                            ? " style='text-align: " + cell.getHorizontalAlignment().name().toLowerCase() + ";'"
                            : "";
                    tableBuilder.append("<th").append(style).append(">");
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
                    String style = cell.getHorizontalAlignment() != Table.HorizontalAlignment.LEFT
                            ? " style='text-align: " + cell.getHorizontalAlignment().name().toLowerCase() + ";'"
                            : "";
                    tableBuilder.append("<td").append(span).append(style).append(">");
                    if ("asciidoc".equals(cell.getStyle())) {
                        tableBuilder.append(cell.getContent());
                    } else {
                        Object content = cell.getContent();
                        if (content instanceof RubyArray) {
                            for (Object el : (RubyArray<?>) content) {
                                if (el instanceof String) {
                                    tableBuilder.append(formatParagraph((String) el));
                                }
                            }
                        }
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
            return formatParagraph(block.getContent().toString());

        } else if (transform.equals("preamble")) {

            return ((StructuralNode) node).getContent().toString();

        } else if ("admonition".equals(transform)) {
            Block block = (Block) node;

            String type = Optional
                    .ofNullable((String) block.getAttribute("style"))
                    .map(String::toLowerCase)
                    .map(value -> {
                        switch (value) {
                            case "note": return "info";
                            case "important": return "note";
                            case "caution": return "warning";
                            default: return value;
                        }
                    })
                    .orElse("info");

            String content = block.getContentModel().equals("compound")
                    ? block.getContent().toString()
                    : formatParagraph(block.getContent().toString());
            String macroId = UUID.nameUUIDFromBytes(content.getBytes()).toString();

            return String.format("<ac:structured-macro ac:name='%s' ac:schema-version='1' ac:macro-id='%s'>" +
                    "<ac:rich-text-body>%s</ac:rich-text-body>" +
                    "</ac:structured-macro>", type, macroId, content);

        } else if (transform.equals("image")) {
            Block block = (Block) node;

            StringBuilder imageBuilder = new StringBuilder();

            imageBuilder
                    .append("<p>")
                    .append("<ac:image");

            Optional.ofNullable(block.getAttribute("align"))
                    .ifPresent(align -> imageBuilder.append(" ac:align='").append(align).append("'"));

            Optional.ofNullable(block.getAttribute("width"))
                    .ifPresent(width -> imageBuilder.append(" ac:width='").append(width).append("'"));

            String path = Stream.of(block.getAttribute("imagesdir"), block.getAttribute("target"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .reduce((str1, str2) -> Path.of(str1, str2).toString())
                    .orElse(null);

            imageBuilder
                    .append(">")

                    .append("<ri:attachment ri:filename='").append(path).append("'/>")
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

        } else if (transform.equals("olist") || transform.equals("colist")) {

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

        } else if (node instanceof ListItem) {

            ListItem item = (ListItem) node;

            if (item.hasText()) {
                StringBuilder builder = new StringBuilder(formatParagraph(item.getText()));
                for (StructuralNode itemNode : item.getBlocks()) {
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
            builder.append("<![CDATA[").append(replaceCalloutSymbols(block.getSource())).append("]]>");
            builder.append("</ac:plain-text-body>");

            builder.append("</ac:structured-macro>");

            return builder.toString();

        } else {
            log.warn("Node '{}: {}' with transform '{}' ignored",
                    node.getClass().getSimpleName(), node.getNodeName(), transform);
        }

        return null;
    }


    private static final Pattern PATTERN_CALLOUTS = Pattern.compile("(<\\d+>\\s*)+$", Pattern.MULTILINE);
    private static final Pattern PATTERN_CALLOUT = Pattern.compile("<(\\d+)>");

    private static final Map<String, String> CALLOUTS = Map.ofEntries(
            Map.entry("1", "❶"),
            Map.entry("2", "❷"),
            Map.entry("3", "❸"),
            Map.entry("4", "❹"),
            Map.entry("5", "❺"),
            Map.entry("6", "❻"),
            Map.entry("7", "❼"),
            Map.entry("8", "❽"),
            Map.entry("9", "❾"),
            Map.entry("10", "❿"),
            Map.entry("11", "⓫"),
            Map.entry("12", "⓬"),
            Map.entry("13", "⓭"),
            Map.entry("14", "⓮"),
            Map.entry("15", "⓯"),
            Map.entry("16", "⓰"),
            Map.entry("17", "⓱"),
            Map.entry("18", "⓲"),
            Map.entry("19", "⓳"),
            Map.entry("20", "⓴")
    );

    private String replaceCalloutSymbols(String source) {

        StringBuilder result = new StringBuilder();
        Matcher matcher = PATTERN_CALLOUTS.matcher(source);

        while (matcher.find()) {
            String replacement = PATTERN_CALLOUT
                    .matcher(matcher.group())
                    .replaceAll(match -> CALLOUTS.getOrDefault(match.group(1), "🯄"));
            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String formatParagraph(String text) {

        return "<p>" + text
                .replaceAll(LINE_SEPARATOR, " ")
                .replaceAll("\\s*<br/>\\s*", "<br/>")
                .trim()
                + "</p>\n";
    }

    private String formatSectionTitle(Section section) {

        String title = null;

        boolean needsNumber = section.isNumbered() && Optional
                .ofNullable((String) section.getDocument().getAttribute("sectnumlevels"))
                .map(Integer::parseInt)
                .filter(maxLevel -> section.getLevel() > maxLevel)
                .isEmpty();

        if (needsNumber) {
            Section current = section;

            do {
                title = Stream.of(current.getNumeral(), title)
                        .filter(org.apache.commons.lang3.StringUtils::isNotEmpty)
                        .collect(Collectors.joining("."));

                current = current.getParent() instanceof Section
                        ? (Section) current.getParent()
                        : null;

            } while (current != null);
        }

        return Stream.of(title, section.getTitle())
                .filter(org.apache.commons.lang3.StringUtils::isNotEmpty)
                .collect(Collectors.joining(". "));
    }

    private Stream<String> columnStyles(Collection<Column> columns, int tableWidth) {

        int sum = columns.stream().mapToInt(Column::getWidth).sum();
        return columns
                .stream()
                .mapToInt(Column::getWidth)
                .map(width -> tableWidth * width / sum)
                .mapToObj(ConfluenceConverter::formatWidth);
    }

    private static String disclaimer(String disclaimer) {

        String macroId = UUID.nameUUIDFromBytes(disclaimer.getBytes()).toString();

        return String.format("<ac:structured-macro ac:name='%s' ac:schema-version='1' ac:macro-id='%s'>" +
                "<ac:rich-text-body>%s</ac:rich-text-body>" +
                "</ac:structured-macro>", "note", macroId, disclaimer);
    }

    private static String anchor(String anchorId) {

        assert anchorId != null;

        UUIDComposer composer = new UUIDComposer(UUID.fromString("7413e515-3d6f-40e9-9ed4-0157f9afee9b"));
        UUID uuidExpand = UUID.nameUUIDFromBytes(anchorId.getBytes());
        UUID uuidAnchor = composer.compose(uuidExpand);

        return "<ac:structured-macro ac:name='anchor' ac:schema-version='1' ac:macro-id='" + uuidAnchor + "'>" +
                "<ac:parameter ac:name=''>" + anchorId + "</ac:parameter>" +
                "</ac:structured-macro>";
    }

    private static String toc(String documentTitle) {

        UUIDComposer composer = new UUIDComposer(UUID.fromString("9d6d6c0b-bb40-488f-9f0b-bd4829ce1bc8"));
        UUID uuidExpand = UUID.nameUUIDFromBytes(documentTitle.getBytes());
        UUID uuidToc = composer.compose(uuidExpand);

        return "<ac:structured-macro ac:name='expand' ac:schema-version='1' ac:macro-id='" + uuidExpand + "'>" +
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
