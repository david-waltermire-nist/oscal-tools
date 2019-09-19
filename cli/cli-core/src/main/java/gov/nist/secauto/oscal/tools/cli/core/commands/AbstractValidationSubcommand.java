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
package gov.nist.secauto.oscal.tools.cli.core.commands;

import gov.nist.secauto.oscal.tools.cli.core.operations.ValidationFinding;
import gov.nist.secauto.oscal.tools.cli.core.operations.XMLOperations;
import gov.nist.secauto.oscal.tools.cli.framework.ExitCode;
import gov.nist.secauto.oscal.tools.cli.framework.ExitStatus;
import gov.nist.secauto.oscal.tools.cli.framework.InvalidArgumentException;
import gov.nist.secauto.oscal.tools.cli.framework.command.AbstractCommandHandler;
import gov.nist.secauto.oscal.tools.cli.framework.command.CommandContext;

import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.Source;

public abstract class AbstractValidationSubcommand extends AbstractCommandHandler {
  private static final Logger log = LogManager.getLogger(AbstractValidationSubcommand.class);
  private static final String COMMAND = "validate";

  public AbstractValidationSubcommand() {
    super();
  }

  @Override
  public String getName() {
    return COMMAND;
  }

  @Override
  protected String getExtraArgumentsText() {
    return "<file to validate>";
  }

  @Override
  protected void validateOptions(CommandLine cmdLine, CommandContext callingContext) throws InvalidArgumentException {
    List<String> extraArgs = cmdLine.getArgList();
    if (extraArgs.size() != 1) {
      throw new InvalidArgumentException("The source to validate must be provided.");
    }

    File source = new File(extraArgs.get(0));
    if (!source.exists()) {
      throw new InvalidArgumentException("The provided source '" + source.getPath() + "' does not exist.");
    }
    if (!source.canRead()) {
      throw new InvalidArgumentException("The provided source '" + source.getPath() + "' is not readable.");
    }
  }

  @Override
  protected ExitStatus executeCommand(CommandLine cmdLine) {
    List<String> extraArgs = cmdLine.getArgList();

    File file = new File(extraArgs.get(0));

    List<Source> schemaSources;
    try {
      schemaSources = getSchemaSources();
    } catch (IOException e) {
      return ExitCode.PROCESSING_ERROR.toExitStatus(e.getMessage());
    }

    List<ValidationFinding> findings;
    try {
      findings = XMLOperations.validate(file, schemaSources);
    } catch (SAXException e) {
      return ExitCode.PROCESSING_ERROR.toExitStatus(e.getMessage());
    } catch (IOException e) {
      return ExitCode.PROCESSING_ERROR.toExitStatus(e.getMessage());
    }

    ExitStatus retval;
    if (!findings.isEmpty()) {
      log.info("The file '{}' had {} validation issue(s). The issues are:", file.getPath(), findings.size());

      for (ValidationFinding finding : findings) {
        log.info("  [{}] file={}, line={}, column={}, message={}", finding.getSeverity(), finding.getSystemId(),
            finding.getLineNumber(), finding.getColumnNumber(), finding.getMessage());
      }
      retval = ExitCode.FAIL.toExitStatus();
    } else {
      log.info("The file '" + file.getPath() + "' is valid.");
      retval = ExitCode.OK.toExitStatus();
    }
    return retval;
  }

  protected abstract List<Source> getSchemaSources() throws IOException;

}
