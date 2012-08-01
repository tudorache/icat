package org.coode.owl.rdf;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLRuntimeException;

import java.util.Set;
/*
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 25-Jul-2007<br><br>
 */
public class TestURIsWithNumericFragments extends AbstractRendererAndParserTestCase {


    protected Set<OWLAxiom> getAxioms() {
        throw new OWLRuntimeException("TODO:");
//        OWLOntology ont = null;
//        try {
//            URL url = TestURIsWithNumericFragments.class.getResource("/numericfragments.rdf");
//            ont = getManager().loadOntologyFromOntologyDocument(url.toURI());
//            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
//            for(OWLOntologyAnnotationAxiom ax : ont.getOntologyAnnotationAxioms()) {
//                changes.add(new RemoveAxiom(ont, ax));
//            }
//            getManager().applyChanges(changes);
//            getManager().removeOntology(ont.getIRI());
//        }
//        catch(OWLOntologyChangeException e) {
//            throw new RuntimeException(e);
//        }
//        catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//        catch (OWLOntologyCreationException e) {
//            throw new RuntimeException(e);
//        }
//        return ont.getAxioms();
    }


    protected String getClassExpression() {
        return "URIs with purely numeric fragments";
    }
}