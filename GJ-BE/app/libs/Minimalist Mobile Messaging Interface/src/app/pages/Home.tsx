import { AlertTriangle, CloudRainWind, Navigation, ChevronRight, Wind } from "lucide-react";
import { cn } from "../components/Layout";

export function Home() {
  return (
    <div className="flex flex-col gap-6 pt-6 animate-in fade-in duration-500">
      <header className="flex flex-col gap-4">
        <h1 className="text-[32px] font-bold tracking-tight text-[#111111]">홈</h1>
      </header>

      {/* 실시간 재난 상황 (Disaster Alert Card) */}
      <section className="flex flex-col gap-2">
        <h2 className="text-[13px] font-medium text-[#8E8E93] tracking-wide uppercase px-2">실시간 특보</h2>
        <div className="bg-white rounded-[24px] p-5 shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03]">
          <div className="flex items-start gap-4">
            <div className="bg-[#FF3B30] text-white p-2.5 rounded-[14px] shadow-sm">
              <AlertTriangle size={24} strokeWidth={2} />
            </div>
            <div className="flex-1">
              <h3 className="text-[#111111] font-bold text-[18px] leading-tight tracking-tight mb-1">
                호우 경보 발령
              </h3>
              <p className="text-[#3C3C43] text-[15px] leading-relaxed font-medium">
                서울 및 수도권 일대 강한 비. 하천변 및 저지대 접근을 삼가고 안전한 곳으로 대피하시기 바랍니다.
              </p>
              <div className="mt-3 flex items-center gap-2 text-[13px] font-semibold text-[#FF3B30]">
                <span>오늘 14:00 발표</span>
                <span className="opacity-50">•</span>
                <span>행정안전부</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 추천 대피소 (Recommended Shelters) */}
      <section className="flex flex-col gap-2">
        <div className="flex justify-between items-end px-2">
          <h2 className="text-[13px] font-medium text-[#8E8E93] tracking-wide uppercase">가까운 대피소</h2>
          <button className="text-[13px] font-semibold text-[#0A84FF] flex items-center">
            모두 보기
          </button>
        </div>
        
        <div className="bg-white rounded-[24px] shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03] overflow-hidden flex flex-col">
          {/* Shelter Item 1 */}
          <div className="flex items-center justify-between p-4 border-b border-[#E5E5EA]">
            <div className="flex flex-col gap-1">
              <h4 className="font-semibold text-[17px] text-[#111111] tracking-tight">서초구민 체육센터</h4>
              <span className="text-[14px] text-[#8E8E93] font-medium">지진, 호우 겸용 • 수용인원 500명</span>
            </div>
            <div className="flex flex-col items-end gap-2">
              <div className="text-[#8E8E93] font-semibold text-[14px]">
                1.2km
              </div>
              <button className="bg-[#F2F2F7] text-[#0A84FF] rounded-full p-2 active:bg-[#E5E5EA] transition-colors">
                <Navigation size={16} strokeWidth={2.5} />
              </button>
            </div>
          </div>

          {/* Shelter Item 2 */}
          <div className="flex items-center justify-between p-4">
            <div className="flex flex-col gap-1">
              <h4 className="font-semibold text-[17px] text-[#111111] tracking-tight">방배2동 주민센터</h4>
              <span className="text-[14px] text-[#8E8E93] font-medium">실내 구호소 • 수용인원 150명</span>
            </div>
            <div className="flex flex-col items-end gap-2">
              <div className="text-[#8E8E93] font-semibold text-[14px]">
                2.5km
              </div>
              <button className="bg-[#F2F2F7] text-[#0A84FF] rounded-full p-2 active:bg-[#E5E5EA] transition-colors">
                <Navigation size={16} strokeWidth={2.5} />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* 기상 상황 (Weather News) */}
      <section className="flex flex-col gap-2 pb-6">
        <h2 className="text-[13px] font-medium text-[#8E8E93] tracking-wide uppercase px-2">오늘 기상 요약</h2>
        <div className="bg-white rounded-[24px] p-5 shadow-[0_2px_8px_rgba(0,0,0,0.02)] border border-black/[0.03] flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="bg-[#F2F2F7] text-[#0A84FF] p-3 rounded-[16px]">
              <CloudRainWind size={28} strokeWidth={2} />
            </div>
            <div>
              <h4 className="font-semibold text-[#111111] text-[17px] tracking-tight">많은 비 예상</h4>
              <p className="text-[14px] text-[#8E8E93] font-medium">시간당 30mm의 집중호우</p>
            </div>
          </div>
          <div className="text-right flex flex-col items-end">
            <span className="text-[28px] font-bold text-[#111111] tracking-tighter">22°</span>
            <div className="text-[13px] text-[#8E8E93] font-semibold mt-0.5 flex items-center gap-1">
              <Wind size={14} /> 5m/s
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
