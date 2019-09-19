/**
 * Portions of this software was developed by employees of the National Institute
 * of Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 17 United States Code Section 105, works of NIST employees are
 * not subject to copyright protection in the United States and are considered to
 * be in the public domain. Permission to freely use, copy, modify, and distribute
 * this software and its documentation without fee is hereby granted, provided that
 * this notice and disclaimer of warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
 * THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND FREEDOM FROM
 * INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE
 * SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR FREE. IN NO EVENT
 * SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT,
 * INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM, OR
 * IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY,
 * CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR
 * PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT
 * OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package gov.nist.secauto.oscal.tools.cli.framework.command;

import gov.nist.secauto.oscal.tools.cli.framework.ExitStatus;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractParentCommandHandler extends AbstractCommandHandler {
  private static final Logger log = LogManager.getLogger(AbstractCommandHandler.class);

  private final Map<String, CommandHandler> commandToSubcommandHandlerMap = new LinkedHashMap<>();

  public void addCommandHandler(CommandHandler handler) {
    String commandName = handler.getName();
    this.commandToSubcommandHandlerMap.put(commandName, handler);
  }

  @Override
  public ExitStatus processCommand(String[] args, CommandContext callingContext) {
    log.debug("Processing command: {}", getName());
    String subcommandName = args[0];
    CommandHandler subcommandHandler = commandToSubcommandHandlerMap.get(subcommandName);

    ExitStatus retval;
    if (subcommandHandler == null) {
      retval = super.processCommand(args, callingContext);
    } else {
      retval = subcommandHandler.processCommand(Arrays.copyOfRange(args, 1, args.length),
          new CommandContext(subcommandHandler, callingContext));
    }
    return retval;
  }

  protected void showHelp(CommandLine cmdLine, CommandContext callingContext) {
    if (commandToSubcommandHandlerMap.isEmpty()) {
      super.showHelp(cmdLine, callingContext);
      return;
    }

    StringBuilder builder = new StringBuilder();
    builder.append(callingContext.getExec());

    for (CommandHandler handler : callingContext.getCallingCommands()) {
      builder.append(" ");
      builder.append(handler.getName());
    }

    builder.append(" <command>");
    String callingSyntax = builder.toString();

    builder.append(" [<args>]");
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(builder.toString(), getOptions());
    System.out.println();
    System.out.println("The following are available subcommands:");
    for (CommandHandler handler : commandToSubcommandHandlerMap.values()) {
      System.out.printf("   %-12.12s %-60.60s%n", handler.getName(), handler.getDescription());
    }
    System.out.println();
    System.out.println("'" + callingSyntax + " --help' will show help on that specific command.");
  }
}
