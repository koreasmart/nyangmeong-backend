package com.nyangmeong.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 케어미션 배치 스위치]
 * - app.care-mission.batch-enabled=true 일 때만 배치 빈 등록
 */
class CareMissionJobSwitchTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(CareMissionService.class, () -> mock(CareMissionService.class))
            .withBean(ObservationFake.class)
            .withUserConfiguration(CareMissionJob.class);

    @Test
    void 키가_없으면_배치를_등록하지_않는다() {
        runner.run(context -> assertThat(context).doesNotHaveBean(CareMissionJob.class));
    }

    @Test
    void false면_배치를_등록하지_않는다() {
        runner.withPropertyValues("app.care-mission.batch-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CareMissionJob.class));
    }

    @Test
    void true면_배치를_등록한다() {
        runner.withPropertyValues("app.care-mission.batch-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CareMissionJob.class));
    }
}
