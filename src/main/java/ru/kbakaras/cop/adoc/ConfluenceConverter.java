package ru.kbakaras.cop.adoc;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.converter.ConverterFor;
import org.asciidoctor.converter.StringConverter;

import java.util.Map;

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
                default:
                    return phrase.getText();
            }

        } else if (node instanceof Table) {
            Table table = (Table) node;

            StringBuilder builder = new StringBuilder();
            builder.append("<table>\n");
            builder.append("<tbody>\n");

            table.getHeader().forEach(row -> {
                builder.append("<tr>\n");
                row.getCells().forEach(cell -> {
                    builder.append("<th>");
                    builder.append(cell.getText());
                    builder.append("</th>\n");
                });
                builder.append("</tr>\n");
            });

            table.getBody().forEach(row -> {
                builder.append("<tr>\n");
                row.getCells().forEach(cell -> {
                    builder.append("<td>");
                    builder.append(cell.getText());
                    builder.append("</td>\n");
                });
                builder.append("</tr>\n");
            });

            builder.append("\n</tbody>");
            builder.append("\n</table>\n");

            return builder.toString();

        } else if (transform.equals("paragraph")) {
            StructuralNode block = (StructuralNode) node;
            String content = block.getContent().toString();

            return "<p>" + content.replaceAll(LINE_SEPARATOR, " ") + "</p>\n";

        } else if (transform.equals("preamble")) {

            return ((StructuralNode) node).getContent().toString();

        } else if (transform.equals("image")) {
            Block block = (Block) node;

            return "<p>\n" +
                    "<ac:image>\n" +
                    "<ri:attachment ri:filename='" + block.getAttribute("target") + "'/>\n" +
                    "</ac:image>\n" +
                    "</p>\n";
        }

        return null;
    }

}
