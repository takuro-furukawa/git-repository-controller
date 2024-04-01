package org.example.gitrepocontroller.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * git info object for web page
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitInfo {
    String repoUrl;
    String branchName;
    List<BranchInfo> branchInfoList;
    List<CommitInfo> commitInfoList;
}
