package com.example.blog.media.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.blog.media.config.CosProperties;

class TencentCosObjectStorageTests {

    @Test
    void buildsCosPublicUrlAndRequiresCredentials() {
        CosProperties properties = new CosProperties(
                "ap-shanghai",
                "blog-1250000000",
                "secret-id",
                "secret-key",
                "",
                "blog/prod",
                10_485_760,
                12_000,
                12_000
        );

        TencentCosObjectStorage storage = new TencentCosObjectStorage(properties);

        assertThat(storage.configured()).isTrue();
        assertThat(storage.publicUrl("blog/prod/hello world.png"))
                .isEqualTo("https://blog-1250000000.cos.ap-shanghai.myqcloud.com/blog/prod/hello%20world.png");
        assertThat(new CosProperties("ap-shanghai", "blog-1250000000", "", "", "", "blog/prod", 1, 1, 1)
                .configured()).isFalse();
    }
}
