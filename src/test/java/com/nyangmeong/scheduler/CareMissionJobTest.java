package com.nyangmeong.scheduler;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 케어미션 정산·출제 배치]
 */
class CareMissionJobTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final ObservationFake observationFake = mock(ObservationFake.class);
    private final CareMissionJob job = new CareMissionJob(careMissionService, observationFake);

    @Test
    void 전날_정산을_먼저_하고_관찰_중_반려동물을_넘겨_오늘_출제한다() {
        when(observationFake.search(TODAY)).thenReturn(Set.of(10L));

        job.run(TODAY);

        InOrder order = inOrder(careMissionService);
        order.verify(careMissionService).settle(TODAY.minusDays(1));
        order.verify(careMissionService).issue(TODAY, Set.of(10L));
    }
}
