package edu.stanford.bmir.protege.web.client.perspective;


import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TabBar;
import edu.stanford.bmir.protege.web.client.Messages;
import edu.stanford.bmir.protege.web.client.action.AbstractUiAction;
import edu.stanford.bmir.protege.web.client.form.LanguageMapCurrentLocaleMapper;
import edu.stanford.bmir.protege.web.client.library.popupmenu.PopupMenu;
import edu.stanford.bmir.protege.web.shared.perspective.PerspectiveDescriptor;
import edu.stanford.bmir.protege.web.shared.perspective.PerspectiveId;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 23/06/2014
 */
public class PerspectiveSwitcherViewImpl extends Composite implements PerspectiveSwitcherView {

    interface PerspectiveSwitcherViewImplUiBinder extends UiBinder<HTMLPanel, PerspectiveSwitcherViewImpl> {

    }

    private static PerspectiveSwitcherViewImplUiBinder ourUiBinder = GWT.create(PerspectiveSwitcherViewImplUiBinder.class);

    @UiField
    protected TabBar tabBar;

    @UiField
    protected Button newTabButton;


    private Optional<PerspectiveId> highlightedPerspective = Optional.empty();

    private final List<PerspectiveDescriptor> displayedPerspectives = Lists.newArrayList();

    private final PerspectiveLinkFactory linkFactory;

    private final List<PerspectiveDescriptor> bookmarkedPerspectives = new ArrayList<>();


    private PerspectiveLinkActivatedHandler linkActivatedHandler = perspectiveId -> {};

    private AddPerspectiveLinkRequestHandler addPerspectiveLinkRequestHandler = () -> {};

    private AddBookmarkedPerspectiveLinkHandler addBookMarkedPerspectiveLinkHandler = perspectiveId -> {};

    private RemovePerspectiveLinkRequestHandler removePerspectiveLinkRequestHandler = perspectiveId -> {};

    private ResetPerspectiveToDefaultStateHandler resetPerspectiveToDefaultStateHandler = perspectiveId -> {};

    private AddViewHandler addViewHandler = perspectiveId -> {};

    private boolean addPerspectiveAllowed = true;

    private boolean closePerspectiveAllowed = true;

    private boolean addViewAllowed = true;

    private static Messages messages = GWT.create(Messages.class);

    private final LanguageMapCurrentLocaleMapper localeMapper;

    @Inject
    public PerspectiveSwitcherViewImpl(PerspectiveLinkFactory linkFactory, LanguageMapCurrentLocaleMapper localeMapper) {
        this.linkFactory = linkFactory;
        this.localeMapper = localeMapper;
        HTMLPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }

    @UiHandler("tabBar")
    protected void handlePerspectiveLinkClicked(BeforeSelectionEvent<Integer> event) {
        /*
          Veto the selection if it does not correspond to the highlighted link
         */
        PerspectiveId link = displayedPerspectives.get(event.getItem()).getPerspectiveId();
        if (!highlightedPerspective.equals(Optional.of(link))) {
            event.cancel();
        }
    }

    @UiHandler("newTabButton")
    protected void handleNewPerspectiveButtonClicked(ClickEvent clickEvent) {
        if(!addPerspectiveAllowed) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu();
        for (final PerspectiveDescriptor perspectiveDescriptor : bookmarkedPerspectives) {
            AbstractUiAction action = new AbstractUiAction(localeMapper.getValueForCurrentLocale(perspectiveDescriptor.getLabel())) {
                @Override
                public void execute() {
                    addBookMarkedPerspectiveLinkHandler.handleAddBookmarkedPerspective(perspectiveDescriptor);
                }
            };
            action.setEnabled(!displayedPerspectives.contains(perspectiveDescriptor));
            popupMenu.addItem(action);
        }
        popupMenu.addSeparator();
        popupMenu.addItem(messages.perspective_addBlankTab() + "\u2026",
                          () -> addPerspectiveLinkRequestHandler.handleAddNewPerspectiveLinkRequest());
        popupMenu.showRelativeTo(newTabButton);
    }

    public void setPerspectiveLinks(List<PerspectiveDescriptor> perspectives) {
        removeAllDisplayedPerspectives();
        for (final PerspectiveDescriptor perspectiveDescriptor : perspectives) {
            addPerspectiveLink(perspectiveDescriptor);
        }
        ensureHighlightedPerspectiveLinkIsSelected();
    }

    @Override
    public void addPerspectiveLink(final PerspectiveDescriptor perspectiveDescriptor) {
        PerspectiveId perspectiveId = perspectiveDescriptor.getPerspectiveId();
        this.displayedPerspectives.add(perspectiveDescriptor);
        PerspectiveLink linkWidget = linkFactory.createPerspectiveLink(perspectiveId);
        linkWidget.setLabel(localeMapper.getValueForCurrentLocale(perspectiveDescriptor.getLabel()));
        linkWidget.addClickHandler(event -> {
            GWT.log("[PerspectiveSwitcherViewImpl] link clicked");
            highlightedPerspective = Optional.of(perspectiveId);
            linkActivatedHandler.handlePerspectiveLinkActivated(perspectiveId);
        });
        if (addViewAllowed) {
            linkWidget.addActionHandler(messages.perspective_addView(), () -> {
                if (addViewAllowed) {
                    addViewHandler.handleAddViewToPerspective(perspectiveId);
                }
            });
        }
        linkWidget.addActionHandler(messages.perspective_reset(),
                                    () -> resetPerspectiveToDefaultStateHandler.handleResetPerspectiveToDefaultState(perspectiveId));
        if (closePerspectiveAllowed) {
            linkWidget.addActionHandler(messages.perspective_close(),
                                        () -> {
                                            if (closePerspectiveAllowed) {
                                                removePerspectiveLinkRequestHandler.handleRemovePerspectiveLinkRequest(perspectiveId);
                                            }
                                        });
        }
        tabBar.addTab(linkWidget.asWidget());
    }

    @Override
    public void removePerspectiveLink(PerspectiveDescriptor perspectiveDescriptor) {

        int index = displayedPerspectives.indexOf(perspectiveDescriptor);
        if (index == -1) {
            return;
        }
        displayedPerspectives.remove(perspectiveDescriptor);
        tabBar.removeTab(index);
    }

    private void removeAllDisplayedPerspectives() {
        while (tabBar.getTabCount() > 0) {
            tabBar.removeTab(0);
        }
        this.displayedPerspectives.clear();
    }

    @Override
    public void setAddBookMarkedPerspectiveLinkHandler(AddBookmarkedPerspectiveLinkHandler handler) {
        this.addBookMarkedPerspectiveLinkHandler = handler;
    }

    @Override
    public void setBookmarkedPerspectives(List<PerspectiveDescriptor> perspectives) {
        this.bookmarkedPerspectives.clear();
        this.bookmarkedPerspectives.addAll(perspectives);
    }

    public List<PerspectiveDescriptor> getPerspectiveLinks() {
        return Lists.newArrayList(displayedPerspectives);
    }

    public void setPerspectiveLinkActivatedHandler(PerspectiveLinkActivatedHandler handler) {
        linkActivatedHandler = checkNotNull(handler);
    }

    public void setAddPerspectiveLinkRequestHandler(AddPerspectiveLinkRequestHandler handler) {
        addPerspectiveLinkRequestHandler = checkNotNull(handler);
    }

    public void setRemovePerspectiveLinkHandler(RemovePerspectiveLinkRequestHandler handler) {
        removePerspectiveLinkRequestHandler = checkNotNull(handler);
    }

    @Override
    public void setResetPerspectiveToDefaultStateHandler(ResetPerspectiveToDefaultStateHandler handler) {
        resetPerspectiveToDefaultStateHandler = checkNotNull(handler);
    }

    @Override
    public void setAddViewHandler(AddViewHandler handler) {
        addViewHandler = checkNotNull(handler);
    }

    public void setHighlightedPerspective(PerspectiveId perspectiveId) {
        checkNotNull(perspectiveId);
        highlightedPerspective = Optional.of(perspectiveId);
        ensureHighlightedPerspectiveLinkIsSelected();
    }

    public Optional<PerspectiveId> getSelectedPerspective() {
        return highlightedPerspective;
    }

    private void ensureHighlightedPerspectiveLinkIsSelected() {
        if (!highlightedPerspective.isPresent()) {
            return;
        }
        for (int i = 0; i < displayedPerspectives.size(); i++) {
            if (displayedPerspectives.get(i).getPerspectiveId().equals(highlightedPerspective.get())) {
                if (tabBar.getSelectedTab() != i) {
                    tabBar.selectTab(i);
                }
                break;
            }
        }
    }

    @Override
    public void setAddPerspectiveAllowed(boolean addPerspectiveAllowed) {
        newTabButton.setVisible(addPerspectiveAllowed);
        this.addPerspectiveAllowed = addPerspectiveAllowed;
    }

    @Override
    public void setClosePerspectiveAllowed(boolean closePerspectiveAllowed) {
        this.closePerspectiveAllowed = closePerspectiveAllowed;
    }

    @Override
    public void setAddViewAllowed(boolean addViewAllowed) {
        this.addViewAllowed = addViewAllowed;
    }
}
