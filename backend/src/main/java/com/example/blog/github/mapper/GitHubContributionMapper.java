package com.example.blog.github.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.github.model.GitHubContributionSnapshot;

@Mapper
public interface GitHubContributionMapper {

    Optional<GitHubContributionSnapshot> findSnapshot();

    int upsertSnapshot(
            @Param("username") String username,
            @Param("totalContributions") int totalContributions,
            @Param("contributionsJson") String contributionsJson
    );
}
