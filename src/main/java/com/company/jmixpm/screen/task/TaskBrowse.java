package com.company.jmixpm.screen.task;

import com.company.jmixpm.entity.Project;
import com.company.jmixpm.entity.Task;
import com.company.jmixpm.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.screen.*;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.upload.TemporaryStorage;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UiController("Task_.browse")
@UiDescriptor("task-browse.xml")
@LookupComponent("tasksTable")
public class TaskBrowse extends StandardLookup<Task> {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Downloader downloader;

    @Autowired
    private FileStorageUploadField uploadTasks;

    @Autowired
    private TemporaryStorage temporaryStorage;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private CollectionLoader<Task> tasksDl;

    @Install(to = "tasksTable.attachment", subject = "columnGenerator")
    private Component tasksTableAttachmentColumnGenerator(Task task) {
        if (task.getAttachment() != null) {
            LinkButton linkButton = uiComponents.create(LinkButton.class);
            linkButton.setCaption(task.getAttachment().getFileName());
            linkButton.addClickListener(clickEvent -> {
                downloader.download(task.getAttachment());
            });
            return linkButton;
        }
        return null;
    }

    @Subscribe("uploadTasks")
    public void onUploadTasksFileUploadSucceed(SingleFileUploadField.FileUploadSucceedEvent event) throws IOException {
        UUID fileId = uploadTasks.getFileId();
        File file = temporaryStorage.getFile(fileId);

        List<String> taskNames = FileUtils.readLines(file, StandardCharsets.UTF_8);

        List<Task> importedTasks = new ArrayList<>(taskNames.size());

        for (String name : taskNames) {
            Task task = dataManager.create(Task.class);
            task.setName(name);
            task.setAssignee((User) currentAuthentication.getUser());
            task.setProject(loadDefaultProject());

            importedTasks.add(task);
        }

        dataManager.save(importedTasks.toArray());
        tasksDl.load();
    }

    @Nullable
    private Project loadDefaultProject() {
        return dataManager.load(Project.class)
                .query("select p from Project p where p.defaultProject = :defaultProject")
                .parameter("defaultProject", true)
                .optional().orElse(null);
    }
    

}