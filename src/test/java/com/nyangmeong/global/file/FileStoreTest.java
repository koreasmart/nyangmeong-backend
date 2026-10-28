package com.nyangmeong.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 파일 저장·읽기]
 */
class FileStoreTest {

    @TempDir
    Path root;

    private final AtchFileMapper atchFileMapper = mock(AtchFileMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private FileStore fileStore;

    @BeforeEach
    void setUp() {
        SystemSetting setting = new SystemSetting();
        setting.setFileMaxSz(10L);
        when(systemSettingService.get()).thenReturn(setting);
        doAnswer(invocation -> {
            invocation.<AtchFile>getArgument(0).setAtchFileSn(7L);
            return null;
        }).when(atchFileMapper).insert(any());
        fileStore = new FileStore(atchFileMapper, new FileRoot(root), systemSettingService);
    }

    @Test
    void 이미지를_디스크와_ATCH_FILE에_저장한다() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("photo", "kongi.PNG", "image/png", png);

        Long atchFileSn = fileStore.store(file, "1");

        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        AtchFile saved = captor.getValue();
        assertThat(atchFileSn).isEqualTo(7L);
        assertThat(saved.getFileExtnCd()).isEqualTo(FileExtension.PNG.getCode());
        assertThat(saved.getOrgnlFileNm()).isEqualTo("kongi.PNG");
        assertThat(saved.getFileSz()).isEqualTo((long) png.length);
        assertThat(saved.getRgtrId()).isEqualTo("1");
        assertThat(Files.readAllBytes(root.resolve(saved.getPhysStrgPath()))).containsExactly(png);
    }

    @Test
    void 확장자와_실제_형식이_다르면_FILE_TYPE_NOT_ALLOWED() {
        MockMultipartFile pdfAsPng = new MockMultipartFile("photo", "a.png", "image/png",
                new byte[] {'%', 'P', 'D', 'F', '-', '1'});
        MockMultipartFile textAsJpg = new MockMultipartFile("photo", "a.jpg", "image/jpeg", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> fileStore.store(pdfAsPng, "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> fileStore.store(textAsJpg, "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        verify(atchFileMapper, never()).insert(any());
    }

    @Test
    void 허용하지_않는_확장자는_FILE_TYPE_NOT_ALLOWED() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[] {1});
        MockMultipartFile gif = new MockMultipartFile("photo", "a.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> fileStore.store(pdf, "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> fileStore.store(gif, "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        verify(atchFileMapper, never()).insert(any());
    }

    @Test
    void 설정_상한을_넘으면_FILE_TOO_LARGE() {
        MockMultipartFile big = new MockMultipartFile("photo", "a.jpg", "image/jpeg", new byte[11]);

        assertThatThrownBy(() -> fileStore.store(big, "1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 저장한_파일을_읽는다() {
        Long atchFileSn = fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "character-1.png", "SYSTEM");
        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        AtchFile saved = captor.getValue();
        saved.setUseYn("Y");
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);

        StoredFile loaded = fileStore.load(atchFileSn);

        assertThat(loaded.content()).containsExactly(9, 8);
        assertThat(loaded.contentType()).isEqualTo("image/png");
    }

    @Test
    void 없는_파일은_FILE_NOT_FOUND() {
        assertThatThrownBy(() -> fileStore.load(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 루트_밖_경로는_읽지_않는다() {
        AtchFile escaped = new AtchFile();
        escaped.setFileExtnCd(FileExtension.PNG.getCode());
        escaped.setPhysStrgPath("../outside.png");
        when(atchFileMapper.selectInUse(5L)).thenReturn(escaped);

        assertThatThrownBy(() -> fileStore.load(5L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 공개_파일은_public_아래에_저장하고_공개로_읽는다() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("photo", "post.png", "image/png", png);

        Long atchFileSn = fileStore.storePublic(file, "1");

        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        AtchFile saved = captor.getValue();
        assertThat(saved.getPhysStrgPath()).startsWith(FileStore.PUBLIC_PREFIX);
        assertThat(Files.readAllBytes(root.resolve(saved.getPhysStrgPath()))).containsExactly(png);
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);
        assertThat(fileStore.loadPublic(atchFileSn).content()).containsExactly(png);
    }

    @Test
    void 비공개_파일은_공개로_읽으면_FILE_NOT_FOUND() {
        Long atchFileSn = fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "receipt.png", "1");
        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(captor.getValue());

        assertThat(captor.getValue().getPhysStrgPath()).doesNotStartWith(FileStore.PUBLIC_PREFIX);
        assertThatThrownBy(() -> fileStore.loadPublic(atchFileSn))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 없는_번호는_공개로_읽어도_FILE_NOT_FOUND() {
        assertThatThrownBy(() -> fileStore.loadPublic(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }
}
