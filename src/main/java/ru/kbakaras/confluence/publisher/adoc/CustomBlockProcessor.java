package ru.kbakaras.confluence.publisher.adoc;

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Reader;

import java.util.Map;

@Contexts({Contexts.PARAGRAPH})
@ContentModel(ContentModel.SIMPLE)
public class CustomBlockProcessor extends BlockProcessor {
    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        return parent.convert();
    }
}
