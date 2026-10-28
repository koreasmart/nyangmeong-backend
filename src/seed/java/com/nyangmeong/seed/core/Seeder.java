package com.nyangmeong.seed.core;

/**
 * [도메인별 시드]
 * - SeedDataApplication이 정한 순서로 부른다. 앞 Seeder가 World에 남긴 값을 쓴다
 */
public interface Seeder {

    void run(Seed seed, World world);
}
