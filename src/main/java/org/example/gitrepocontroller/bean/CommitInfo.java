package org.example.gitrepocontroller.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * git commit info
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitInfo {
    String commitId;
    String commitMessage;
}
