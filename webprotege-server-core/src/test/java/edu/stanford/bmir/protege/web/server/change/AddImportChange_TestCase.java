package edu.stanford.bmir.protege.web.server.change;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.change.AddImportData;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-28
 */
@RunWith(MockitoJUnitRunner.class)
public class AddImportChange_TestCase<R> {

    private AddImportChange change;

    @Mock
    private OWLOntologyID ontologyId;

    @Mock
    private OWLImportsDeclaration importsDeclaration;

    @Mock
    private Set<OWLEntity> signature = Collections.singleton(mock(OWLEntity.class));

    @Mock
    private OWLObjectDuplicator duplicator;

    @Mock
    private OntologyChangeVisitor visitor;

    @Mock
    private OntologyChangeVisitorEx<R> changeVisitorEx;

    @Before
    public void setUp() {
        change = AddImportChange.of(ontologyId, importsDeclaration);
    }

    @Test
    public void shouldGetSuppliedOntologyId() {
        assertThat(change.getOntologyId(), is(ontologyId));
    }

    @Test
    public void shouldGetSuppliedDeclaration() {
        assertThat(change.getImportsDeclaration(), is(importsDeclaration));
    }

    @Test
    public void shouldGetSignature() {
        assertThat(change.getSignature(), is(empty()));
    }

    @Test
    public void shouldReturnIsAxiomChange() {
        assertThat(change.isAxiomChange(), is(false));
    }

    @Test
    public void shouldReturnIsAddAxiom() {
        assertThat(change.isAddAxiom(), is(false));
    }

    @Test
    public void shouldReturnIsRemoveAxiom() {
        assertThat(change.isRemoveAxiom(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldGetAxiomOrThrow() {
        change.getAxiomOrThrow();
    }

    @Test
    public void shouldReturnIsNotChangeForAxiomType() {
        AxiomType.AXIOM_TYPES.forEach(axiomType -> assertThat(change.isChangeFor(axiomType), is(false)));
    }

    @Test
    public void shouldReplaceIris() {
        assertThat(change.replaceIris(duplicator), is(change));
    }

    @Test
    public void shouldVisitChange() {
        change.accept(visitor);
        verify(visitor, times(1)).visit(change);
    }

    @Test
    public void shouldVisitChangeEx() {
        change.accept(changeVisitorEx);
        verify(changeVisitorEx, times(1)).visit(change);
    }

    @Test
    public void shouldReturnIsRemoveOntologyAnnotation() {
        assertThat(change.isRemoveOntologyAnnotation(), is(false));
    }

    @Test
    public void shouldReturnIsAddOntologyAnnotation() {
        assertThat(change.isAddOntologyAnnotation(), is(false));
    }

    @Test
    public void shouldCreateOwlOntologyChangeRecord() {
        var changeRecord = change.toOwlOntologyChangeRecord();
        assertThat(changeRecord.getOntologyID(), is(ontologyId));
        assertThat(changeRecord.getData(), is(new AddImportData(importsDeclaration)));
    }
}
