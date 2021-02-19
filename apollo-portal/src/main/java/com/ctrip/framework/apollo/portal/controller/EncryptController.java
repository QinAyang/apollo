package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.security.EncryptUtil;
import com.ctrip.framework.apollo.common.security.RsaKey;
import com.ctrip.framework.apollo.common.security.RsaTool;
import com.ctrip.framework.apollo.common.utils.Result;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * the controller of Encrypt/Decrypt
 * <p>
 *
 * @author Ning.Zhang(Pegasus)
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize        2/14/2021        Initialize  *
 * *****************************************************************
 */
@RestController
@RequestMapping("/encrypt")
public class EncryptController {

    @Value("${apollo.encrypt.app_id}")
    private String apolloEncryptAppId;
    @Value("${apollo.encrypt.private_key_name}")
    private String apolloEncryptPrivateKeyName;
    @Value("${apollo.encrypt.public_key_name}")
    private String apolloEncryptPublicKeyName;

    private final ReleaseService releaseService;

    public EncryptController(final ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @PostMapping(value = "getKeyAppId")
    public Result<String> getKeyAppId() {
        return Result.success(this.apolloEncryptAppId);
    }

    @RequestMapping(value = "generateKey")
    public void generateKey(@RequestParam(value = "appId") final String appId,
                            final HttpServletResponse response) throws Exception {
        if (!this.apolloEncryptAppId.equals(appId)) {
            return;
        }
        final RsaKey rsaKey = RsaTool.generatorKeyPair();
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("公钥秘钥.txt", "UTF-8"));

        final OutputStream outputStream = response.getOutputStream();

        final StringBuilder content = new StringBuilder();
        content.append("********************************************************************");
        content.append("\n");
        content.append(String.format("*                 公钥Key的名称必须是: %s                           *", this.apolloEncryptPublicKeyName));
        content.append("\n");
        content.append(String.format("*                 私钥Key的名称必须是: %s                          *", this.apolloEncryptPrivateKeyName));
        content.append("\n");
        content.append("*                                                                                            *");
        content.append("\n");
        content.append("*                                     例子:                                                *");
        content.append("\n");
        content.append(String.format("*                 %s = MIICdgIBADANBg...                         *", this.apolloEncryptPublicKeyName));
        content.append("\n");
        content.append(String.format("*                 %s = MIGfMA0GCSqGSI...                       *", this.apolloEncryptPrivateKeyName));
        content.append("\n");
        content.append("*                                                                                            *");
        content.append("\n");
        content.append(String.format("*   密文必须使用%s%s格式进行包含, 以便区别于明文, 方便解密   *", EncryptUtil.PREFIX, EncryptUtil.SUFFIX));
        content.append("\n");
        content.append("*                                     例子:                                                *");
        content.append("\n");
        content.append(String.format("*                %s                                                    *", EncryptUtil.addTag("sPpI6Ib9...")));
        content.append("\n");
        content.append("********************************************************************");
        content.append("\n");
        content.append("\n");
        content.append("公钥:");
        content.append("\n");
        content.append(rsaKey.getPublicKey());
        content.append("\n");
        content.append("\n");
        content.append("私钥:");
        content.append("\n");
        content.append(rsaKey.getPrivateKey());
        content.append("\n");
        outputStream.write(content.toString().trim().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "doEncrypt")
    public Result<EncryptResult> encrypt(@RequestParam(value = "value") final String value,
                                         @RequestParam(value = "appId") final String appId,
                                         @RequestParam(value = "env") final String env,
                                         @RequestParam(value = "cluster") final String cluster,
                                         @RequestParam(value = "namespace") final String namespace) {
        try {
            final RsaKey rsaKey = getRsaKey(env, cluster, namespace);
            final EncryptResult encryptResult = new EncryptResult();
            final String encrypt = RsaTool.encrypt(value, rsaKey.getPublicKey().trim());
            encryptResult.setEncryptValue(encrypt);
            encryptResult.setEncryptConfigValue(EncryptUtil.addTag(encrypt));
            return Result.success(encryptResult);
        } catch (final Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping(value = "doDecrypt")
    public Result<String> decrypt(@RequestParam(value = "value") String value,
                                  @RequestParam(value = "appId") final String appId,
                                  @RequestParam(value = "env") final String env,
                                  @RequestParam(value = "cluster") final String cluster,
                                  @RequestParam(value = "namespace") final String namespace) {
        if (!this.apolloEncryptAppId.equals(appId)) {
            return Result.error("权限不足");
        }

        try {
            final RsaKey rsaKey = getRsaKey(env, cluster, namespace);
            final List<Exception> exceptionList = new ArrayList<>();
            String result = EncryptUtil.decrypt(value, rsaKey, exceptionList);
            if(!exceptionList.isEmpty()){
                throw exceptionList.get(0);
            }
            return Result.success(result);
        } catch (final Exception exception) {
            return Result.error(exception.getMessage());
        }
    }

    private RsaKey getRsaKey(final String env,
                             final String cluster,
                             final String namespace) {
        RsaKey result = null;
        final List<ReleaseBO> allReleases = this.releaseService.findAllReleases(
                this.apolloEncryptAppId,
                Env.valueOf(env),
                cluster,
                namespace,
                0,
                1);

        if (null != allReleases && !allReleases.isEmpty()) {
            result = new RsaKey();
            for (final ReleaseBO releaseBO : allReleases) {
                for (final KVEntity item : releaseBO.getItems()) {
                    if (item.getKey().equals(this.apolloEncryptPrivateKeyName)) {
                        result.setPrivateKey(item.getValue());
                    } else if (item.getKey().equals(this.apolloEncryptPublicKeyName)) {
                        result.setPublicKey(item.getValue());
                    }
                }
            }
        }

        if (null == result || ObjectUtils.isEmpty(result.getPrivateKey()) || ObjectUtils.isEmpty(result.getPublicKey())) {
            throw new RuntimeException("请先配置公钥和私钥");
        }
        return result;
    }

    public static class EncryptResult {
        private String encryptValue;
        private String encryptConfigValue;

        public String getEncryptValue() {
            return encryptValue;
        }

        public void setEncryptValue(final String encryptValue) {
            this.encryptValue = encryptValue;
        }

        public String getEncryptConfigValue() {
            return encryptConfigValue;
        }

        public void setEncryptConfigValue(final String encryptConfigValue) {
            this.encryptConfigValue = encryptConfigValue;
        }
    }
}