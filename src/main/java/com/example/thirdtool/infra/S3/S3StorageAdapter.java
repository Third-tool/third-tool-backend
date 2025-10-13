package com.example.thirdtool.infra.S3;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.infra.adapter.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageAdapter implements FileStoragePort {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png");

    @Override
    public String uploadFile(MultipartFile file, String folderPath) {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;
        String key = folderPath + "/" + fileName;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                                                          .bucket(bucketName)
                                                          .key(key)
                                                          .contentType(file.getContentType())
                                                          .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAIL);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                                                                   .bucket(bucketName)
                                                                   .key(key)
                                                                   .build();

            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.FILE_DELETE_FAIL);
        }
    }

    // ===== 내부 유틸 =====
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_UNSUPPORTED_EXTENSION);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) throw new BusinessException(ErrorCode.FILE_UNSUPPORTED_EXTENSION);
        return filename.substring(lastDot + 1);
    }

    private String extractKeyFromUrl(String fileUrl) {
        int idx = fileUrl.indexOf(".com/");
        if (idx == -1) throw new BusinessException(ErrorCode.INVALID_INPUT);
        return fileUrl.substring(idx + 5);
    }
}