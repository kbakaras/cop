package ru.kbakaras.cop;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "publish",
        mixinStandardHelpOptions = true,
        header = "Operation for initial publication of page to Confluence"
)
public class PublishCommand implements Callable<Void> {

    @CommandLine.ParentCommand
    private ConfluencePublisher parent;


    @Override
    public Void call() throws Exception {
        return null;
    }

}
