<template>
  <div class="profile-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>个人中心</h2>
      <p class="subtitle">管理你的个人信息和账号设置</p>
    </div>

    <!-- 个人信息表单 -->
    <div class="profile-form-container">
      <a-card title="基本信息" bordered :style="{ width: '100%', maxWidth: '800px', margin: '0 auto' }">
        <a-form
          ref="profileFormRef"
          :model="profileForm"
          layout="vertical"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          @finish="handleSave"
        >
          <!-- 头像上传 -->
          <a-form-item label="用户头像">
            <!-- 注：若后端无单独头像上传接口，需先对接OSS等存储服务，替换action地址 -->
            <a-upload
              list-type="picture-card"
              :show-upload-list="false"
              :before-upload="handleBeforeUpload"
              action="https://your-oss-upload-url.com"
            @change="handleAvatarChange"
            >
            <img v-if="profileForm.userAvatar" :src="profileForm.userAvatar" alt="用户头像" class="avatar-img" />
            <div v-else class="upload-placeholder">
              <UserOutlined class="upload-icon" />
              <div class="upload-text">上传头像</div>
            </div>
            </a-upload>
            <p class="upload-hint">暂不支持更换头像</p>
          </a-form-item>

          <!-- 用户名 -->
          <a-form-item
            label="用户名"
            name="userName"
            :rules="[
              { required: true, message: '请输入用户名', trigger: 'blur' },
              { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' },
              { max: 20, message: '用户名不超过 20 个字符', trigger: 'blur' }
            ]"
          >
            <a-input
              v-model:value="profileForm.userName"
              placeholder="请输入用户名"
              :disabled="isLoading"
            />
          </a-form-item>

          <!-- 用户账号（只读，不允许修改） -->
          <a-form-item label="用户账号">
            <a-input
              v-model:value="profileForm.userAccount"
              placeholder="用户账号"
              disabled
            />
          </a-form-item>

          <!-- 个人简介（若后端支持，否则可删除） -->
          <a-form-item
            label="个人简介"
            name="userProfile"
            :rules="[
              { max: 200, message: '个人简介不超过 200 个字符', trigger: 'blur' }
            ]"
          >
            <a-textarea
              v-model:value="profileForm.userProfile"
              rows="4"
              placeholder="请输入个人简介（可选）"
              :disabled="isLoading"
            />
          </a-form-item>

          <!-- 保存按钮 -->
          <a-form-item :wrapper-col="{ offset: 6, span: 16 }">
            <a-button
              type="primary"
              html-type="submit"
              :loading="isLoading"
            >
              保存修改
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 修复后：值用普通导入，类型用 import type
import { message } from 'ant-design-vue';
import type { FormInstance, UploadFile, UploadProps } from 'ant-design-vue';
import { useLoginUserStore } from '@/stores/loginUser.ts'
// 导入现有 userController API 接口
import { getLoginUser, updateUser } from '@/api/userController.ts'

// 1. 路由实例
const router = useRouter()
// 2. 用户状态存储
const loginUserStore = useLoginUserStore()
// 3. 表单实例（用于校验和提交）
const profileFormRef = ref<FormInstance | null>(null)
// 4. 加载状态（控制按钮和输入框禁用）
const isLoading = ref(false)

// 5. 个人信息表单数据（与后端 UserUpdateRequest 字段匹配）
const profileForm = reactive({
  id: 0,                  // 用户ID（必传，后端用于定位修改对象）
  userAccount: '',         // 用户账号（只读，不修改）
  userName: '',            // 用户名（可修改）
  userAvatar: '',          // 用户头像URL（可修改）
  userProfile: ''          // 个人简介（可修改，若后端支持）
})

// 6. 页面加载时：获取当前登录用户信息（复用 getLoginUser 接口）
onMounted(async () => {
  // 校验登录状态：未登录则跳转登录页
  const currentLoginUser = loginUserStore.loginUser
  if (!currentLoginUser?.id) {
    message.warning('请先登录后访问个人中心')
    await router.push({
      path: '/user/login',
      query: { redirect: router.currentRoute.value.fullPath } // 登录后重定向回个人中心
    })
    return
  }

  // 调用接口获取用户信息
  try {
    isLoading.value = true
    const res = await getLoginUser() // 无需传参，后端自动识别当前登录用户

    // 接口成功：回显数据到表单
    if (res.data.code === 0 && res.data.data) {
      const userData = res.data.data as API.LoginUserVO
      profileForm.id = userData.id || 0
      profileForm.userAccount = userData.userAccount || ''
      profileForm.userName = userData.userName || '无名用户'
      profileForm.userAvatar = userData.userAvatar || ''
      profileForm.userProfile = (userData as any).userProfile || '' // 若 LoginUserVO 无此字段，需后端补充
    } else {
      message.error('获取个人信息失败：' + (res.data.message || '服务器异常'))
    }
  } catch (error) {
    console.error('获取用户信息异常：', error)
    message.error('获取个人信息失败，请刷新页面重试')
  } finally {
    isLoading.value = false
  }
})

// 7. 头像上传前校验（格式+大小）
const handleBeforeUpload: UploadProps['beforeUpload'] = (file: UploadFile) => {
  // 校验格式：仅允许 JPG/PNG
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isImage) {
    message.error('请上传 JPG 或 PNG 格式的图片')
    return false
  }

  // 校验大小：不超过 2MB
  const isLt2M = (file.size ?? 0) / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB，请选择更小的图片')
    return false
  }

  return true // 允许上传
}

// 8. 头像上传结果处理（临时存储 URL，保存时一并提交）
const handleAvatarChange: UploadProps['onChange'] = (info: { file: UploadFile }) => {
  if (info.file.status === 'done') {
    // 注：此处需根据存储服务返回格式调整（示例：OSS 返回的 URL 在 response.data 中）
    const avatarUrl = info.file.response?.data || ''
    if (avatarUrl) {
      profileForm.userAvatar = avatarUrl
      message.success('头像上传成功，点击「保存修改」生效')
    } else {
      message.error('头像上传失败：未获取到图片地址')
    }
  } else if (info.file.status === 'error') {
    message.error('头像上传失败：' + (info.file.response?.message || '网络异常'))
  }
}

// 9. 保存个人信息（复用 updateUser 接口）
const handleSave = async () => {
  // 表单校验：确保必填项符合规则
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch (error) {
    // 校验失败：Ant Design Vue 会自动提示，无需额外处理
    return
  }

  // 构造后端需要的更新参数（与 UserUpdateRequest 字段匹配）
  const updateParams: API.UserUpdateRequest = {
    id: profileForm.id,                // 必传：用户ID
    userName: profileForm.userName,    // 要修改的用户名
    userAvatar: profileForm.userAvatar,// 要修改的头像URL（若上传了新头像）
    userProfile: profileForm.userProfile // 要修改的个人简介（若后端支持）
    // 排除不需要修改的字段（如 userAccount，后端通常不允许修改）
  }

  // 调用接口提交更新
  try {
    isLoading.value = true
    const res = await updateUser(updateParams)

    // 接口成功：提示+更新全局状态
    if (res.data.code === 0 && res.data.data) {
      message.success('个人信息保存成功！')

      // 更新全局登录用户状态（同步头部导航的用户名/头像）
      const currentLoginUser = loginUserStore.loginUser
      loginUserStore.setLoginUser({
        ...currentLoginUser,
        userName: profileForm.userName,
        userAvatar: profileForm.userAvatar,
        userProfile: profileForm.userProfile
      })
    } else {
      message.error('保存失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('保存个人信息异常：', error)
    message.error('保存失败，请检查网络或联系管理员')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
/* 页面整体样式 */
.profile-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px); /* 减去头部高度，避免滚动条异常 */
}

/* 页面标题样式 */
.page-header {
  margin-bottom: 32px;
  text-align: center;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-header .subtitle {
  font-size: 14px;
  color: #6b7280;
}

/* 表单容器样式 */
.profile-form-container {
  background-color: #ffffff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 头像样式 */
.avatar-img {
  width: 128px;
  height: 128px;
  border-radius: 50%;
  object-fit: cover; /* 保持图片比例，避免拉伸变形 */
  border: 1px solid #eee;
}

/* 头像上传占位符样式 */
.upload-placeholder {
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: 1px dashed #d0d7de;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-placeholder:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
}

/* 头像上传提示文本 */
.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}
</style>
