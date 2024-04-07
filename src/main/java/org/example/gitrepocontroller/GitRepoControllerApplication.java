package org.example.gitrepocontroller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.example.gitrepocontroller.bean.BranchInfo;
import org.example.gitrepocontroller.bean.CommitInfo;
import org.example.gitrepocontroller.bean.GitInfo;
import org.example.gitrepocontroller.bean.PageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

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

    @Value("${git.credential.username}")
    private String USERNAME;
    @Value("${git.credential.password}")
    private String PASSWORD;

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
    public String getBranch1(Model model, PageInfo pageInfo) throws IOException, GitAPIException, URISyntaxException {
        log.debug("get repo");
        GitInfo gitInfo = pageInfo.getGitInfoFrom();
        Git git = getGit(gitInfo.getRepoUrl(), true);
        gitInfo.setBranchInfoList(getBranch(git));

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
    public String getLogs1(Model model, PageInfo pageInfo) throws GitAPIException, IOException, URISyntaxException {
        GitInfo gitInfo = pageInfo.getGitInfoFrom();
        log.debug("get branch");
        Git git = getGit(gitInfo.getRepoUrl(), true);
        gitInfo.setCommitInfoList(getLog(git, gitInfo.getBranchName()));

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
    public String getBranch2(Model model, PageInfo pageInfo) throws IOException, GitAPIException, URISyntaxException {
        log.debug("get repo");
        GitInfo gitInfo = pageInfo.getGitInfoTo();
        Git git = getGit(gitInfo.getRepoUrl(), false);
        gitInfo.setBranchInfoList(getBranch(git));

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
    public String getLogs2(Model model, PageInfo pageInfo) throws GitAPIException, IOException, URISyntaxException {
        GitInfo gitInfo = pageInfo.getGitInfoTo();
        log.debug("get branch");
        Git git = getGit(gitInfo.getRepoUrl(), false);
        gitInfo.setCommitInfoList(getLog(git, gitInfo.getBranchName()));

        model.addAttribute("PageInfo", pageInfo);
        return "index";
    }

    @GetMapping(value = "/", params = "cp")
    public String cherryPick(Model model, PageInfo pageInfo) throws GitAPIException, IOException, URISyntaxException {
        GitInfo gitInfoFrom = pageInfo.getGitInfoFrom();
        Git gitFrom = getGit(gitInfoFrom.getRepoUrl(), true);
        gitFrom.checkout().setName(gitInfoFrom.getBranchName()).call();
        gitFrom.fetch().setRemote("origin").call();
        RevFilter filter = RevFilter.NO_MERGES;
        List<RevCommit> pickedCommitList = new ArrayList<>();
        Arrays.asList(gitInfoFrom.getCheckedCommitId()).forEach(s -> {
            try {
                StreamSupport.stream(gitFrom.log().setRevFilter(filter).call().spliterator(), false)
                        .filter(rc -> rc.getId().getName().equals(s)).forEach(pickedCommitList::add);
            } catch (GitAPIException e) {
                throw new RuntimeException("error while trying to search selected commit", e);
            }
        });

        GitInfo gitInfoTo = pageInfo.getGitInfoTo();
        Git gitTo = getGit(gitInfoTo.getRepoUrl(), false);
        gitTo.remoteAdd().setName("fork").setUri(new URIish(gitInfoFrom.getRepoUrl())).call();
        gitTo.fetch().setRemote("fork").call();
        gitTo.checkout().setName(gitInfoTo.getBranchName()).call();
        pickedCommitList.forEach(rc -> {
            try {
                gitTo.cherryPick().include(rc.getId()).call();
            } catch (GitAPIException e) {
                throw new RuntimeException("error while trying to cherry-pick selected commit", e);
            }
        });

        gitTo.push().setRemote("origin").setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD)).call();

        model.addAttribute("PageInfo", pageInfo);
        return "index";
    }

    private Git getGit(String uri, boolean isFrom) throws GitAPIException, IOException, URISyntaxException {
        String repoName = Paths.get(uri).getFileName().toString();
        Path repoPath = Paths.get(WORKSPACE + "/" + repoName + isFrom);
        Git git;
        if (!repoPath.toFile().exists()) {
            repoPath.toFile().mkdirs();
            git = Git.cloneRepository().setURI(uri).setDirectory(repoPath.toFile()).call();
        } else {
            Path path = Paths.get(repoPath.toFile().getAbsolutePath() + "/.git");
            Repository repo = new FileRepositoryBuilder().setGitDir(path.toFile()).build();
            git = new Git(repo);
        }
        git.remoteAdd().setName("origin").setUri(new URIish(uri)).call();
        return git;
    }

    private List<CommitInfo> getLog(Git git, String branchName) throws GitAPIException {
        git.fetch().setRemote("origin").call();
        AtomicBoolean createBranch = new AtomicBoolean(true);
        git.branchList().call()
                .stream()
                .filter(ref -> ref.getName().contains(branchName))
                        .forEach(ref -> createBranch.set(false));
        git.checkout()
                .setCreateBranch(createBranch.get())
                .setName(branchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint("origin/" + branchName)
                .call();
        List<CommitInfo> commitInfoList = new ArrayList<>();
        git.log().setRevFilter(RevFilter.NO_MERGES).setMaxCount(10).call().forEach(revCommit -> {
            commitInfoList.add(new CommitInfo(revCommit.getId().getName(), revCommit.getFullMessage()));
        });
        return commitInfoList;
    }

    private List<BranchInfo> getBranch(Git git) throws GitAPIException {
        git.fetch().setRemote("origin").call();
        List<BranchInfo> branchInfoList = new ArrayList<>();
        git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
                .stream().filter(ref -> ref.getName().contains("origin"))
                .forEach(ref -> branchInfoList.add(new BranchInfo(ref.getName().replaceFirst("refs/remotes/origin/", ""))));
        return branchInfoList;
    }
}