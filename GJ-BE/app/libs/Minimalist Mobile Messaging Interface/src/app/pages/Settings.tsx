import { User, Moon, Bell, Shield, ChevronRight, Info } from "lucide-react";
import { useState } from "react";
import { cn } from "../components/Layout";

export function Settings() {
  const [darkMode, setDarkMode] = useState(false);
  const [notifications, setNotifications] = useState(true);

  return (
    <div className="flex flex-col gap-6 pt-6 animate-in fade-in duration-500 pb-8">
      <header>
        <h1 className="text-[32px] font-bold tracking-tight text-[#111111]">설정</h1>
      </header>

      {/* Account Section */}
      <section className="flex flex-col gap-2">
        <h2 className="text-[13px] font-medium text-[#8E8E93] tracking-wide uppercase px-4">계정</h2>
        <div className="bg-white rounded-[20px] shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03]">
          <button className="w-full flex items-center justify-between p-4 rounded-[20px] active:bg-[#F2F2F7] transition-colors">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-full bg-[#E5E5EA] flex items-center justify-center text-[#8E8E93]">
                <User size={24} />
              </div>
              <div className="text-left flex flex-col">
                <span className="font-semibold text-[17px] text-[#111111] tracking-tight">로그인 필요</span>
                <span className="text-[14px] text-[#8E8E93] font-medium mt-0.5">계정을 연결하여 가족 위치 공유</span>
              </div>
            </div>
            <ChevronRight size={20} className="text-[#C7C7CC]" />
          </button>
        </div>
      </section>

      {/* App Settings Section */}
      <section className="flex flex-col gap-2">
        <h2 className="text-[13px] font-medium text-[#8E8E93] tracking-wide uppercase px-4">앱 설정</h2>
        <div className="bg-white rounded-[20px] shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03] flex flex-col">
          
          {/* Dark Mode Toggle */}
          <div className="flex items-center justify-between p-4 border-b border-[#E5E5EA]">
            <div className="flex items-center gap-3">
              <div className="bg-[#1C1C1E] text-white p-1.5 rounded-[10px]">
                <Moon size={18} strokeWidth={2} />
              </div>
              <span className="font-medium text-[17px] text-[#111111]">다크 모드</span>
            </div>
            <button 
              onClick={() => setDarkMode(!darkMode)}
              className={cn(
                "w-[50px] h-[30px] rounded-full p-[2px] transition-colors duration-300 relative",
                darkMode ? "bg-[#34C759]" : "bg-[#E5E5EA]"
              )}
            >
              <div className={cn(
                "w-[26px] h-[26px] bg-white rounded-full shadow-[0_3px_8px_rgba(0,0,0,0.15)] transform transition-transform duration-300",
                darkMode ? "translate-x-[20px]" : "translate-x-0"
              )} />
            </button>
          </div>

          {/* Notifications Toggle */}
          <div className="flex items-center justify-between p-4 border-b border-[#E5E5EA]">
            <div className="flex items-center gap-3">
              <div className="bg-[#FF3B30] text-white p-1.5 rounded-[10px]">
                <Bell size={18} strokeWidth={2} />
              </div>
              <span className="font-medium text-[17px] text-[#111111]">재난 알림 수신</span>
            </div>
            <button 
              onClick={() => setNotifications(!notifications)}
              className={cn(
                "w-[50px] h-[30px] rounded-full p-[2px] transition-colors duration-300 relative",
                notifications ? "bg-[#34C759]" : "bg-[#E5E5EA]"
              )}
            >
              <div className={cn(
                "w-[26px] h-[26px] bg-white rounded-full shadow-[0_3px_8px_rgba(0,0,0,0.15)] transform transition-transform duration-300",
                notifications ? "translate-x-[20px]" : "translate-x-0"
              )} />
            </button>
          </div>

          {/* Privacy & Security */}
          <button className="w-full flex items-center justify-between p-4 active:bg-[#F2F2F7] transition-colors rounded-b-[20px]">
            <div className="flex items-center gap-3">
              <div className="bg-[#0A84FF] text-white p-1.5 rounded-[10px]">
                <Shield size={18} strokeWidth={2} />
              </div>
              <span className="font-medium text-[17px] text-[#111111]">개인정보 보호 및 보안</span>
            </div>
            <ChevronRight size={20} className="text-[#C7C7CC]" />
          </button>
        </div>
      </section>

      {/* Other Info */}
      <section className="flex flex-col gap-2">
        <div className="bg-white rounded-[20px] shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03] flex flex-col">
          <button className="w-full flex items-center justify-between p-4 border-b border-[#E5E5EA] active:bg-[#F2F2F7] transition-colors text-left rounded-t-[20px]">
            <div className="flex items-center gap-3">
              <div className="bg-[#8E8E93] text-white p-1.5 rounded-[10px]">
                <Info size={18} strokeWidth={2} />
              </div>
              <span className="font-medium text-[17px] text-[#111111]">공지사항</span>
            </div>
            <ChevronRight size={20} className="text-[#C7C7CC]" />
          </button>
          <div className="w-full flex items-center justify-between p-4 text-left">
            <span className="font-medium text-[17px] text-[#111111] pl-[34px]">앱 버전 정보</span>
            <span className="text-[17px] text-[#8E8E93] mr-1">1.0.0</span>
          </div>
        </div>
      </section>
    </div>
  );
}
