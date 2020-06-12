package edu.stanford.bmir.protege.web.client.form;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import edu.stanford.bmir.protege.web.shared.form.FormPageRequest;
import edu.stanford.bmir.protege.web.shared.form.FormRegionPageChangedHandler;
import edu.stanford.bmir.protege.web.shared.form.FormRegionPageRequest;
import edu.stanford.bmir.protege.web.shared.form.data.*;
import edu.stanford.bmir.protege.web.shared.form.field.*;
import edu.stanford.bmir.protege.web.shared.pagination.Page;
import edu.stanford.bmir.protege.web.shared.pagination.PageRequest;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-11-25
 */
public class GridPresenter implements HasGridColumnVisibilityManager {

    public static final int DEFAULT_PAGE_SIZE = 10;
    @Nonnull
    private final GridView view;

    @Nonnull
    private final GridHeaderPresenter headerPresenter;

    @Nonnull
    private final Provider<GridRowPresenter> rowPresenterProvider;

    @Nonnull
    private final List<GridRowPresenter> rowPresenters = new ArrayList<>();

    @Nonnull
    private final List<GridRowViewContainer> rowContainers = new ArrayList<>();

    @Nonnull
    private GridControlDescriptorDto descriptor = GridControlDescriptorDto.get(ImmutableList.of(),
                                                                         null);

    @Nonnull
    private FormRegionPageChangedHandler formRegionPageChangedHandler = () -> {
    };

    private boolean enabled = true;

    @Nonnull
    private GridColumnVisibilityManager columnVisibilityManager = new GridColumnVisibilityManager();

    private HandlerRegistration visibleColumnsChangedHandlerRegistration = () -> {
    };

    private boolean topLevel = false;

    @Nonnull
    private GridOrderByChangedHandler orderByChangedHandler = () -> {};

    @Inject
    public GridPresenter(@Nonnull GridView view,
                         @Nonnull GridHeaderPresenter headerPresenter,
                         @Nonnull Provider<GridRowPresenter> rowPresenterProvider) {
        this.view = checkNotNull(view);
        this.headerPresenter = headerPresenter;
        this.rowPresenterProvider = rowPresenterProvider;
    }

    public void clearValue() {
        view.clear();
    }

    @Nonnull
    public ImmutableList<FormRegionPageRequest> getPageRequests(@Nonnull FormSubject formSubject,
                                                                @Nonnull FormRegionId formRegionId) {
        int pageNumber = view.getPageNumber();
        PageRequest pageRequest = PageRequest.requestPageWithSize(pageNumber, DEFAULT_PAGE_SIZE);
        FormRegionPageRequest gridPageRequest = FormRegionPageRequest.get(formSubject,
                                                                          formRegionId,
                                                                          FormPageRequest.SourceType.GRID_CONTROL,
                                                                          pageRequest);

        ImmutableList<FormRegionPageRequest> rowRequests = rowPresenters
                .stream()
                .map(GridRowPresenter::getPageRequests)
                .flatMap(ImmutableList::stream)
                .collect(toImmutableList());
        return ImmutableList.<FormRegionPageRequest>builder()
                .add(gridPageRequest)
                .addAll(rowRequests)
                .build();

    }

    public GridControlData getValue() {
        ImmutableList<GridRowData> rows = rowPresenters
                .stream()
                .map(GridRowPresenter::getFormDataValue)
                .collect(toImmutableList());
        Page<GridRowData> page = new Page<>(1, 1, rows, rows.size());
        return GridControlData.get(descriptor.toFormControlDescriptor(), page, ImmutableSet.of());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        view.setEnabled(enabled);
        rowPresenters.forEach(rowPresenter -> rowPresenter.setEnabled(enabled));
        rowContainers.forEach(rowContainer -> rowContainer.setEnabled(enabled));
    }

    public void setFormRegionPageChangedHandler(@Nonnull FormRegionPageChangedHandler formRegionPageChangedHandler) {
        this.formRegionPageChangedHandler = checkNotNull(formRegionPageChangedHandler);
        rowPresenters.forEach(row -> row.setFormRegionPageChangedHandler(formRegionPageChangedHandler));
    }

    public void setTopLevel() {
        this.topLevel = true;
        ImmutableSet<GridColumnId> gridColumnIds = descriptor.getLeafColumns()
                                                             .map(GridColumnDescriptorDto::getId)
                                                             .collect(toImmutableSet());
        columnVisibilityManager.setVisibleColumns(gridColumnIds);
    }


    public void setValue(GridControlDataDto value) {
        rowPresenters.clear();
        view.clear();
        // List of objects
        Page<GridRowDataDto> rowsPage = value.getRows();
        rowsPage.getPageElements()
                .stream()
                .map(this::toGridRowPresenter)
                .forEach(this::addRow);
        view.setPageCount(rowsPage.getPageCount());
        view.setPageNumber(rowsPage.getPageNumber());
        view.setElementCount(rowsPage.getTotalElements());
        view.setPaginatorVisible(rowsPage.getPageCount() > 1);
        view.setEnabled(enabled);
        ImmutableSet<FormRegionOrdering> ordering = value.getOrdering();
        headerPresenter.setOrderBy(ordering);
    }

    private GridRowPresenter toGridRowPresenter(GridRowDataDto rowDataValue) {
        GridRowPresenter rowPresenter = createAndSetupGridRowPresenter();
        rowPresenter.setValue(rowDataValue);
        return rowPresenter;
    }

    private GridRowPresenter createAndSetupGridRowPresenter() {
        GridRowPresenter rowPresenter = rowPresenterProvider.get();
        rowPresenter.setFormRegionPageChangedHandler(formRegionPageChangedHandler);
        rowPresenter.setColumnDescriptors(descriptor);
        rowPresenter.setEnabled(enabled);
        rowPresenter.setColumnVisibilityManager(columnVisibilityManager);
        return rowPresenter;
    }

    private void addRow(GridRowPresenter presenter) {
        GridRowViewContainer rowContainer = view.addRow();
        rowContainer.setEnabled(enabled);
        rowContainer.setDeleteHandler(() -> {
            view.removeRow(rowContainer);
            rowPresenters.remove(presenter);
        });
        presenter.start(rowContainer);
        rowPresenters.add(presenter);
        rowContainers.add(rowContainer);
    }

    public IsWidget getView() {
        return view;
    }

    public void hideHeaderRow() {
        view.hideHeader();
    }

    public void requestFocus() {
        view.requestFocus();
    }

    public void setDescriptor(GridControlDescriptorDto descriptor) {
        if (this.descriptor.equals(descriptor)) {
            return;
        }
        clear();
        this.descriptor = checkNotNull(descriptor);
        headerPresenter.setGridDescriptor(descriptor);
    }

    public void clear() {
        view.clear();
    }

    public void start(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
        headerPresenter.start(view.getHeaderContainer());
        headerPresenter.addFilteredColumnsChangedHandler(source -> {
            // We only take direction from the filter menu if its for the top level grid
            // in the situation where there are nested grids
            if (topLevel) {
                columnVisibilityManager.setVisibleColumns(source.getFilteredColumns());
            }
        });
        view.setPageNumberChangedHandler(pageNumber -> formRegionPageChangedHandler.handleFormRegionPageChanged());
        view.setNewRowHandler(() -> {
            if(!enabled) {
                return;
            }
            GridRowPresenter presenter = createAndSetupGridRowPresenter();
            addRow(presenter);
            presenter.requestFocus();
        });
        headerPresenter.setGridColumnOrderByChangeHandler(this::handleOrderByChanged);
    }

    private void handleOrderByChanged() {
        this.orderByChangedHandler.handleOrderByChanged();
    }

    @Override
    public void setColumnVisibilityManager(@Nonnull GridColumnVisibilityManager columnVisibilityManager) {
        this.columnVisibilityManager = checkNotNull(columnVisibilityManager);
        // Remove any existing handler so that we don't receive multiple events
        this.visibleColumnsChangedHandlerRegistration.removeHandler();
        this.visibleColumnsChangedHandlerRegistration
                = this.columnVisibilityManager.addVisibleColumnsChangedHandler(this::updateVisibleColumns);
        this.headerPresenter.setColumnVisibilityManager(columnVisibilityManager);
        rowPresenters.forEach(row -> row.setColumnVisibilityManager(columnVisibilityManager));
        updateVisibleColumns();
    }

    public void updateVisibleColumns() {
        // Columns in header
        headerPresenter.updateVisibleColumns();
        // Show/hide columns in rows
        rowPresenters.forEach(GridRowPresenter::updateColumnVisibility);
    }

    public ImmutableList<FormRegionOrdering> getOrdering() {
        return headerPresenter.getOrderBy();
    }

    public void setOrderByChangedHandler(GridOrderByChangedHandler orderByChangedHandler) {
        this.orderByChangedHandler = checkNotNull(orderByChangedHandler);
    }
}
