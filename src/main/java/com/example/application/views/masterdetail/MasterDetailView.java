package com.example.application.views.masterdetail;

import com.example.application.data.entity.Projects;
import com.example.application.data.service.ProjectsService;
import com.example.application.views.MainLayout;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Master-Detail")
@Route(value = "master-detail/:projectsID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class MasterDetailView extends Div implements BeforeEnterObserver {

    private final String PROJECTS_ID = "projectsID";
    private final String PROJECTS_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private final Grid<Projects> grid = new Grid<>(Projects.class, false);

    CollaborationAvatarGroup avatarGroup;

    private TextField projectNumber;
    private TextField projectName;
    private DatePicker dateOfBeginn;
    private TextField projectManager;
    private TextField priceNetto;
    private TextField priceBrutto;
    private TextField statusOfProject;
    private TextField comments;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final CollaborationBinder<Projects> binder;

    private Projects projects;

    private final ProjectsService projectsService;

    public MasterDetailView(ProjectsService projectsService) {
        this.projectsService = projectsService;
        addClassNames("master-detail-view");

        // UserInfo is used by Collaboration Engine and is used to share details
        // of users to each other to able collaboration. Replace this with
        // information about the actual user that is logged, providing a user
        // identifier, and the user's real name. You can also provide the users
        // avatar by passing an url to the image as a third parameter, or by
        // configuring an `ImageProvider` to `avatarGroup`.
        UserInfo userInfo = new UserInfo(UUID.randomUUID().toString(), "Steve Lange");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        avatarGroup = new CollaborationAvatarGroup(userInfo, null);
        avatarGroup.getStyle().set("visibility", "hidden");

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("projectNumber").setAutoWidth(true);
        grid.addColumn("projectName").setAutoWidth(true);
        grid.addColumn("dateOfBeginn").setAutoWidth(true);
        grid.addColumn("projectManager").setAutoWidth(true);
        grid.addColumn("priceNetto").setAutoWidth(true);
        grid.addColumn("priceBrutto").setAutoWidth(true);
        grid.addColumn("statusOfProject").setAutoWidth(true);
        grid.addColumn("comments").setAutoWidth(true);
        grid.setItems(query -> projectsService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PROJECTS_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(MasterDetailView.class);
            }
        });

        // Configure Form
        binder = new CollaborationBinder<>(Projects.class, userInfo);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(projectNumber, String.class)
                .withConverter(new StringToIntegerConverter("Only numbers are allowed")).bind("projectNumber");
        binder.forField(priceNetto, String.class)
                .withConverter(new StringToIntegerConverter("Only numbers are allowed")).bind("priceNetto");
        binder.forField(priceBrutto, String.class)
                .withConverter(new StringToIntegerConverter("Only numbers are allowed")).bind("priceBrutto");

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.projects == null) {
                    this.projects = new Projects();
                }
                binder.writeBean(this.projects);
                projectsService.update(this.projects);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(MasterDetailView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> projectsId = event.getRouteParameters().get(PROJECTS_ID).map(Long::parseLong);
        if (projectsId.isPresent()) {
            Optional<Projects> projectsFromBackend = projectsService.get(projectsId.get());
            if (projectsFromBackend.isPresent()) {
                populateForm(projectsFromBackend.get());
            } else {
                Notification.show(String.format("The requested projects was not found, ID = %d", projectsId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(MasterDetailView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        projectNumber = new TextField("Project Number");
        projectName = new TextField("Project Name");
        dateOfBeginn = new DatePicker("Date Of Beginn");
        projectManager = new TextField("Project Manager");
        priceNetto = new TextField("Price Netto");
        priceBrutto = new TextField("Price Brutto");
        statusOfProject = new TextField("Status Of Project");
        comments = new TextField("Comments");
        formLayout.add(projectNumber, projectName, dateOfBeginn, projectManager, priceNetto, priceBrutto,
                statusOfProject, comments);

        editorDiv.add(avatarGroup, formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Projects value) {
        this.projects = value;
        String topic = null;
        if (this.projects != null && this.projects.getId() != null) {
            topic = "projects/" + this.projects.getId();
            avatarGroup.getStyle().set("visibility", "visible");
        } else {
            avatarGroup.getStyle().set("visibility", "hidden");
        }
        binder.setTopic(topic, () -> this.projects);
        avatarGroup.setTopic(topic);

    }
}
