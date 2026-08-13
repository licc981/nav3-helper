# Maven Central 发布

本项目发布以下坐标：

- `io.github.licc981:navigation3-helper:<version>`
- `io.github.licc981:nav3-ksp-compiler:<version>`

版本统一由 `gradle/libs.versions.toml` 中的 `navHelper` 定义。Maven Central 的版本不可覆盖，
每次发布前必须使用一个从未发布过的新版本。

## 推荐方式：GitHub Actions

换电脑时不需要迁移本地发布环境。将下列值配置为 GitHub 仓库的
`Settings > Secrets and variables > Actions > Repository secrets`：

| Secret | 内容 |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal User Token 的 username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal User Token 的 password |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored GPG 私钥全文 |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG 私钥密码 |

`SIGNING_IN_MEMORY_KEY` 必须包含 `BEGIN PGP PRIVATE KEY BLOCK` 和
`END PGP PRIVATE KEY BLOCK` 两行。Secret 只保存在 GitHub，不应写入 Git、工作流或项目配置。
发布脚本会从私钥自动读取 key ID，不需要单独维护对应的 Secret。

首次设置或凭据轮换：

1. 登录 [Maven Central Portal](https://central.sonatype.com/)，确认 `io.github.licc981`
   namespace 已验证，并创建 User Token。
2. 准备 GPG 私钥。已有密钥可执行
   `gpg --armor --export-secret-keys <KEY_ID>` 导出用于 GitHub Secret 的文本。
3. 将上述四项 Repository secrets 配置到 GitHub。
4. 在 `gradle/libs.versions.toml` 更新 `navHelper`，提交并推送到 `main`。

推送到 `main` 且发布相关文件发生变化时，工作流会自动读取 `navHelper` 并发布。也可以打开
`Actions > Publish to Maven Central > Run workflow` 手动触发，输入的版本必须与 `navHelper` 一致。

工作流在 macOS runner 上构建 iOS/JVM/Wasm 产物，运行两模块 JVM 测试、生成 POM，随后上传并
自动发布。输入版本与项目版本不一致时会直接终止。

## 本地发布

新电脑需要 Java 17、Git 和 GPG。将凭据放在用户级
`~/.gradle/gradle.properties`，不要放进本仓库：

```properties
mavenCentralUsername=<portal-token-username>
mavenCentralPassword=<portal-token-password>
signingInMemoryKey=<ASCII-armored-private-key-with-newlines>
signingInMemoryKeyPassword=<gpg-key-password>
# 可选；省略时发布脚本会从 signingInMemoryKey 自动读取
signingInMemoryKeyId=<key-id>
```

在 `.properties` 文件中，私钥换行需表示为 `\n`。更简单的方式是通过下面的环境变量传入
原始多行私钥。

也可以使用同名 Gradle 项目环境变量：

```text
ORG_GRADLE_PROJECT_mavenCentralUsername
ORG_GRADLE_PROJECT_mavenCentralPassword
ORG_GRADLE_PROJECT_signingInMemoryKey
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
# 可选；省略时发布脚本会从 signingInMemoryKey 自动读取
ORG_GRADLE_PROJECT_signingInMemoryKeyId
```

更新 `navHelper` 后执行：

```shell
bash scripts/publish-maven-central.sh <version>
```

脚本会检查版本、Central 中是否已存在同版本和必要配置，先运行测试及 POM 校验，再发布两个
模块。它不会打印或保存凭据。

## 发布后验证

Central 公共仓库可能需要几分钟到几十分钟完成同步。可检查：

- `https://repo.maven.apache.org/maven2/io/github/licc981/navigation3-helper/<version>/`
- `https://repo.maven.apache.org/maven2/io/github/licc981/nav3-ksp-compiler/<version>/`

如果 Gradle 已显示部署进入 publishing 状态，不要因为短时间 404 而重复上传同一版本。

## 安全与备份

- Portal Token 曾在聊天、日志或终端中明文出现时，应立即撤销并生成新 Token。
- 仅备份 GPG 私钥和密码到密码管理器或加密存储，不要放入普通网盘或 Git。
- 换电脑后优先使用 GitHub Actions；需要本地发布时，再从安全备份恢复用户级配置。
