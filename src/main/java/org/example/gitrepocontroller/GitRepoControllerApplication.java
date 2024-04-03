package org.example.gitrepocontroller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.example.gitrepocontroller.bean.BranchInfo;
import org.example.gitrepocontroller.bean.CommitInfo;
import org.example.gitrepocontroller.bean.GitInfo;
import org.example.gitrepocontroller.bean.PageInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
     *
     * @param model
     * @return
     */
    @GetMapping(value = "/")
    public String home(Model model) {
        model.addAttribute("PageInfo", new PageInfo(new GitInfo(), new GitInfo()));
        log.debug("init page");
        if (!WORKSPACE_PATH.toFile().exists()) {
            WORKSPACE_PATH.toFile().mkdirs();
        }

        return "index";
    }

    /**
     * get list of branch for repo1
     *
     * @param model
     * @param pageInfo
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @GetMapping(value = "/", params = "repo1")
    public String getBranch1(Model model, PageInfo pageInfo) throws IOException, GitAPIException {
        log.debug("get repo");
        GitInfo gitInfo = pageInfo.getGitInfoFrom();
        Git git = getGit(gitInfo.getRepoUrl());
        git.pull().setRemote("origin").call();
        List<BranchInfo> branchInfoList = new ArrayList<>();
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().forEach(ref -> {
            branchInfoList.add(new BranchInfo(ref.getName()));
        });
        gitInfo.setBranchInfoList(branchInfoList);

        model.addAttribute("PageInfo", pageInfo);
        return "index";
    }

    /**
     * get list of log for branch1
     *
     * @param model
     * @param pageInfo
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    @GetMapping(value = "/", params = "branch1")
    public String getLogs1(Model model, PageInfo pageInfo) throws GitAPIException, IOException {
        GitInfo gitInfo = pageInfo.getGitInfoFrom();
        log.debug("get branch");
        Git git = getGit(gitInfo.getRepoUrl());
        git.checkout().setName(gitInfo.getBranchName());
        List<CommitInfo> commitInfoList = new ArrayList<>();
        git.log().setMaxCount(10).call().forEach(revCommit -> {
            commitInfoList.add(new CommitInfo(revCommit.getId().getName(), revCommit.getFullMessage()));
        });
        gitInfo.setCommitInfoList(commitInfoList);

        model.addAttribute("PageInfo", pageInfo);
        return "index";
    }

    /**
     * get list of branch for repo2
     *
     * @param model
     * @param pageInfo
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @GetMapping(value = "/", params = "repo2")
    public String getBranch2(Model model, PageInfo pageInfo) throws IOException, GitAPIException {
        log.debug("get repo");
        GitInfo gitInfo = pageInfo.getGitInfoTo();
        Git git = getGit(gitInfo.getRepoUrl());
        git.pull().setRemote("origin").call();
        List<BranchInfo> branchInfoList = new ArrayList<>();
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().forEach(ref -> {
            branchInfoList.add(new BranchInfo(ref.getName()));
        });
        gitInfo.setBranchInfoList(branchInfoList);

        model.addAttribute("PageInfo", pageInfo);
        return "index";
    }

    /**
     * get list of log for branch2
     *
     * @param model
     * @param pageInfo
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    @GetMapping(value = "/", params = "branch2")
    public String getLogs2(Model model, PageInfo pageInfo) throws GitAPIException, IOException {
        GitInfo gitInfo = pageInfo.getGitInfoTo();
        log.debug("get branch");
        Git git = getGit(gitInfo.getRepoUrl());
        git.checkout().setName(gitInfo.getBranchName());
        List<CommitInfo> commitInfoList = new ArrayList<>();
        git.log().setMaxCount(10).call().forEach(revCommit -> {
            commitInfoList.add(new CommitInfo(revCommit.getId().getName(), revCommit.getFullMessage()));
        });
        gitInfo.setCommitInfoList(commitInfoList);

        model.addAttribute("PageInfo", pageInfo);
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