package org.example.gitrepocontroller;

import ch.qos.logback.classic.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class GitRepoControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitRepoControllerApplication.class, args);
    }

}

@Controller
@RequestMapping
class GitRepoController {

    Log log = LogFactory.getLog("Controller");
    private static final String WORKSPACE = "/tmp/workspace";
    private static final Path WORKSPACE_PATH = Paths.get(WORKSPACE);

    /**
     * show first page
     * @param model
     * @return
     */
    @GetMapping(value = "/")
    public String home(Model model) {
        model.addAttribute("GitInfo", new GitInfo());
        log.debug("init page");
        if (!WORKSPACE_PATH.toFile().exists()) {
            WORKSPACE_PATH.toFile().mkdirs();
        }

        return "index";
    }

    /**
     * get list of branch
     * @param model
     * @param gitInfo
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @GetMapping(value = "/",  params = "repo")
    public String getBranch(Model model, GitInfo gitInfo) throws IOException, GitAPIException {
        model.addAttribute("GitInfo", gitInfo);
        log.debug("get repo");
        Git git = getGit(gitInfo.getRepoUrl());
        git.checkout().setName("main").call();
        git.pull().setRemote("origin").call();
        List<BranchInfo> branchInfoList = new ArrayList<>();
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().forEach(ref -> {
            branchInfoList.add(new BranchInfo(ref.getName()));
        });
        gitInfo.setBranchInfoList(branchInfoList);

        return "index";
    }

    /**
     * get list of log
     * @param model
     * @param gitInfo
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    @GetMapping(value = "/",  params = "branch")
    public String getLogs(Model model, GitInfo gitInfo) throws GitAPIException, IOException {
        model.addAttribute("GitInfo", gitInfo);
        log.debug("get branch");
        Git git = getGit(gitInfo.getRepoUrl());
        git.checkout().setName(gitInfo.getBranchName());
        List<CommitInfo> commitInfoList = new ArrayList<>();
        git.log().setMaxCount(10).call().forEach(revCommit -> {
            commitInfoList.add(new CommitInfo(revCommit.getId().toString(), revCommit.getFullMessage()));
        });
        gitInfo.setCommitInfoList(commitInfoList);

        return "index";
    }

    private Git getGit(String uri) throws GitAPIException, IOException {
        String repoName = Paths.get(uri).getFileName().toString();
        Path repoPath = Paths.get(WORKSPACE + "/" + repoName);
        Git git;
        if (!repoPath.toFile().exists()) {
            repoPath.toFile().mkdirs();
            git = Git.cloneRepository().setURI(uri).setDirectory(repoPath.toFile()).call();
        } else {
            Path path = Paths.get(repoPath.toFile().getAbsolutePath() + "/.git");
            Repository repo = new FileRepositoryBuilder().setGitDir(path.toFile()).build();
            git = new Git(repo);
        }
        return git;
    }
}

/**
 * git info object for web page
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class GitInfo {
    String repoUrl;
    String branchName;
    List<BranchInfo> branchInfoList;
    List<CommitInfo> commitInfoList;
}

@Data
@AllArgsConstructor
class BranchInfo {
    String name;
}

/**
 * git commit info
 */
@Data
@AllArgsConstructor
class CommitInfo {
    String commitId;
    String commitMessage;
}