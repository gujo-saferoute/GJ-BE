package com.example.safe_route_project.home

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safe_route_project.app.ServiceLocator
import com.example.safe_route_project.data.disaster.store.DisasterAlertStore
import kotlinx.coroutines.launch

class HomeAlertBinder(
    private val activity: AppCompatActivity,
    private val titleView: TextView,
    private val messageView: TextView,
    private val sourceView: TextView,
) {
    private val repository by lazy { ServiceLocator.disasterMessageRepository(activity) }
    private val store by lazy { DisasterAlertStore(activity) }

    fun bind() {
        val cachedAlert = store.getCachedAlert()
        if (cachedAlert != null) {
            render(
                title = cachedAlert.title,
                message = cachedAlert.message,
                source = cachedAlert.source,
            )
        } else {
            render(
                title = "재난문자 불러오는 중",
                message = "잠시만 기다려 주세요.",
                source = "행정안전부 연계",
            )
        }

        activity.lifecycleScope.launch {
            runCatching { repository.getLatestAlert() }
                .onSuccess { latest ->
                    if (latest != null) {
                        store.save(latest)
                        render(
                            title = latest.title,
                            message = latest.message,
                            source = latest.source,
                        )
                    } else if (cachedAlert == null) {
                        render(
                            title = "재난문자 없음",
                            message = "현재 표시할 최신 재난문자가 없습니다.",
                            source = "행정안전부 연계",
                        )
                    }
                }
                .onFailure {
                    if (cachedAlert == null) {
                        render(
                            title = "재난문자 로드 실패",
                            message = "네트워크 또는 API 설정을 확인해 주세요.",
                            source = "행정안전부 연계",
                        )
                    }
                }
        }
    }

    private fun render(title: String, message: String, source: String) {
        titleView.text = title
        messageView.text = message
        sourceView.text = source
    }
}