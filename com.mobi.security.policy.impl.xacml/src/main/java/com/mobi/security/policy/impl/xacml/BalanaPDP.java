package com.mobi.security.policy.impl.xacml;

/*-
 * #%L
 * security.policy.impl
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2018 iNovex Information Systems, Inc.
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

import static com.mobi.security.policy.impl.xacml.XACML.POLICY_DENY_OVERRIDES;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_DENY_UNLESS_PERMIT;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_FIRST_APPLICABLE;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_ONLY_ONE_APPLICABLE;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_ORDERED_DENY_OVERRIDES;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_ORDERED_PERMIT_OVERRIDES;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_PERMIT_OVERRIDES;
import static com.mobi.security.policy.impl.xacml.XACML.POLICY_PERMIT_UNLESS_DENY;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Literal;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.security.policy.api.Decision;
import com.mobi.security.policy.api.PDP;
import com.mobi.security.policy.api.PIP;
import com.mobi.security.policy.api.Request;
import com.mobi.security.policy.api.Response;
import com.mobi.security.policy.api.Status;
import com.mobi.security.policy.api.exception.ProcessingException;
import org.w3c.dom.Document;
import org.wso2.balana.Balana;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.combine.PolicyCombiningAlgorithm;
import org.wso2.balana.combine.xacml2.FirstApplicablePolicyAlg;
import org.wso2.balana.combine.xacml2.OnlyOneApplicablePolicyAlg;
import org.wso2.balana.combine.xacml3.DenyOverridesPolicyAlg;
import org.wso2.balana.combine.xacml3.DenyUnlessPermitPolicyAlg;
import org.wso2.balana.combine.xacml3.OrderedDenyOverridesPolicyAlg;
import org.wso2.balana.combine.xacml3.OrderedPermitOverridesPolicyAlg;
import org.wso2.balana.combine.xacml3.PermitOverridesPolicyAlg;
import org.wso2.balana.combine.xacml3.PermitUnlessDenyPolicyAlg;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@Component(immediate = true, provide = {PDP.class, BalanaPDP.class})
public class BalanaPDP implements PDP {

    private List<PIP> pips = new ArrayList<>();
    private BalanaPRP balanaPRP;
    private ValueFactory vf;

    private Balana balana;

    @Activate
    public void setUp() {
        balana = Balana.getInstance();
    }

    @Reference(type = '*', dynamic = true)
    void addPIP(PIP pip) {
        this.pips.add(pip);
    }

    void removePIP(PIP pip) {
        this.pips.remove(pip);
    }

    @Reference
    void setBalanaPRP(BalanaPRP balanaPRP) {
        this.balanaPRP = balanaPRP;
    }

    @Reference
    void setVf(ValueFactory vf) {
        this.vf = vf;
    }

    @Override
    public Request createRequest(IRI subjectId, Map<String, Literal> subjectAttrs, IRI resourceId,
                                 Map<String, Literal> resourceAttrs, IRI actionId, Map<String, Literal> actionAttrs) {
        XACMLRequest.Builder builder = new XACMLRequest.Builder(subjectId, resourceId, actionId, OffsetDateTime.now());
        if (subjectAttrs != null) {
            subjectAttrs.forEach((key, value) -> {
                if (value != null) {
                    builder.addSubjectAttr(key, value);
                }
            });
        }
        if (resourceAttrs != null) {
            resourceAttrs.forEach((key, value) -> {
                if (value != null) {
                    builder.addResourceAttr(key, value);
                }
            });
        }
        if (actionAttrs != null) {
            actionAttrs.forEach((key, value) -> {
                if (value != null) {
                    builder.addActionAttr(key, value);
                }
            });
        }
        return builder.build();
    }

    @Override
    public Response evaluate(Request request) {
        try {
            XACMLRequest xacmlRequest = getRequest(request);
            org.wso2.balana.PDP pdp = getPDP(vf.createIRI(POLICY_DENY_OVERRIDES));
            String result = pdp.evaluate(xacmlRequest.toString());
            return getResponse(result);
        } catch (Exception e) {
            return new XACMLResponse.Builder(Decision.INDETERMINATE, Status.PROCESSING_ERROR)
                    .statusMessage(e.getMessage()).build();
        }
    }

    @Override
    public Response evaluate(Request request, IRI policyAlgorithm) {
        try {
            XACMLRequest xacmlRequest = getRequest(request);
            org.wso2.balana.PDP pdp = getPDP(policyAlgorithm);
            String result = pdp.evaluate(xacmlRequest.toString());
            return getResponse(result);
        } catch (Exception e) {
            return new XACMLResponse.Builder(Decision.INDETERMINATE, Status.PROCESSING_ERROR)
                    .statusMessage(e.getMessage()).build();
        }
    }

    private org.wso2.balana.PDP getPDP(IRI policyAlgorithm) {
        PDPConfig config = balana.getPdpConfig();

        PolicyFinder policyFinder = config.getPolicyFinder();
        Set<PolicyFinderModule> policyFinderModules = new HashSet<>();
        policyFinderModules.add(balanaPRP);
        policyFinder.setModules(policyFinderModules);

        AttributeFinder attributeFinder = config.getAttributeFinder();
        List<AttributeFinderModule> attributeFinderModules = attributeFinder.getModules();
        pips.forEach(pip -> {
            MobiAttributeFinder mobiAttributeFinder = new MobiAttributeFinder(vf, pip);
            attributeFinderModules.add(mobiAttributeFinder);
        });
        attributeFinder.setModules(attributeFinderModules);

        PDPConfig newConfig = new PDPConfig(attributeFinder, policyFinder, null, false);
        balanaPRP.setPDPConfig(newConfig);
        balanaPRP.setCombiningAlg(getAlgorithm(policyAlgorithm));
        return new org.wso2.balana.PDP(newConfig);
    }

    private XACMLResponse getResponse(String responseStr) {
        try (InputStream stream = new ByteArrayInputStream(responseStr.getBytes())) {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            Document doc = docFactory.newDocumentBuilder().parse(stream);
            return new XACMLResponse(doc, vf);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new ProcessingException(e);
        }
    }

    private XACMLRequest getRequest(Request request) {
        if (request instanceof XACMLRequest) {
            return (XACMLRequest) request;
        }
        XACMLRequest.Builder builder = new XACMLRequest.Builder(request.getSubjectId(), request.getResourceId(),
                request.getActionId(), request.getRequestTime());
        request.getSubjectAttrs().forEach(builder::addSubjectAttr);
        request.getResourceAttrs().forEach(builder::addResourceAttr);
        request.getActionAttrs().forEach(builder::addActionAttr);
        return builder.build();
    }

    private PolicyCombiningAlgorithm getAlgorithm(IRI policyAlgorithm) {
        switch (policyAlgorithm.stringValue()) {
            case POLICY_DENY_OVERRIDES:
                return new DenyOverridesPolicyAlg();
            case POLICY_DENY_UNLESS_PERMIT:
                return new DenyUnlessPermitPolicyAlg();
            case POLICY_ORDERED_DENY_OVERRIDES:
                return new OrderedDenyOverridesPolicyAlg();
            case POLICY_ORDERED_PERMIT_OVERRIDES:
                return new OrderedPermitOverridesPolicyAlg();
            case POLICY_PERMIT_OVERRIDES:
                return new PermitOverridesPolicyAlg();
            case POLICY_PERMIT_UNLESS_DENY:
                return new PermitUnlessDenyPolicyAlg();
            case POLICY_FIRST_APPLICABLE:
                return new FirstApplicablePolicyAlg();
            case POLICY_ONLY_ONE_APPLICABLE:
                return new OnlyOneApplicablePolicyAlg();
            default:
                throw new ProcessingException("Policy algorithm " + policyAlgorithm + " not supported");
        }
    }
}
