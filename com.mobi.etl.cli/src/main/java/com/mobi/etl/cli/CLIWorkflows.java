package com.mobi.etl.cli;

/*-
 * #%L
 * com.mobi.etl.cli
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2017 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.mobi.etl.api.ontologies.etl.Workflow;
import com.mobi.etl.api.ontologies.etl.WorkflowFactory;
import com.mobi.etl.api.workflows.WorkflowManager;
import com.mobi.persistence.utils.api.SesameTransformer;
import com.mobi.rdf.api.Model;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

@Command(scope = "mobi", name = "workflows", description = "Manage Workflows deployed in Mobi")
@Service
public class CLIWorkflows implements Action {

    // Service References

    @Reference
    private WorkflowManager workflowManager;

    void setWorkflowManager(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    @Reference
    private SesameTransformer transformer;

    void setTransformer(SesameTransformer transformer) {
        this.transformer = transformer;
    }

    @Reference
    private WorkflowFactory workflowFactory;

    void setWorkflowFactory(WorkflowFactory workflowFactory) {
        this.workflowFactory = workflowFactory;
    }

    // Command Parameters

    @Argument(name = "operation", description = "The action to perform.\n"
            + "mobi:workflows -f/--file <file> deploy - Deploys a Workflow defined in RDF to Mobi.",
            required = true)
    private String operation = null;

    @Option(name = "-f", aliases = "--file", description = "The path to a file containing Workflow RDF")
    private String workflowFile = null;

    // Implementation

    @Override
    public Object execute() throws Exception {
        switch (operation) {
            case "deploy":
                deploy();
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        return null;
    }

    private void deploy() throws Exception {
        File file = new File(workflowFile);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found");
        }
        RDFFormat format = Rio.getParserFormatForFileName(file.getName()).orElseThrow(() ->
                new IOException("Unsupported RDF file type"));
        Model model = transformer.mobiModel(Rio.parse(new FileInputStream(file), "", format));
        Collection<Workflow> workflows = workflowFactory.getAllExisting(model);
        if (workflows.size() == 0 || workflows.size() > 1) {
            throw new IllegalArgumentException("RDF file must contain exactly one Workflow");
        }
        workflowManager.addWorkflow(workflows.iterator().next());
    }
}
