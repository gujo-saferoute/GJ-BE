import { useState } from "react";
import { Accessibility, MapPin, Search, Navigation, Layers, LocateFixed, X } from "lucide-react";
import { cn } from "../components/Layout";

export function MapPage() {
  const [activeCategory, setActiveCategory] = useState("전체");
  const [filterBarrierFree, setFilterBarrierFree] = useState(false);
  const [selectedMarker, setSelectedMarker] = useState<number | null>(null);

  const categories = ["전체", "지진", "호우", "대설", "병원"];

  // Mock markers
  const markers = [
    { id: 1, name: "서초구민 체육센터", type: "지진", distance: "1.2km", capacity: "500명", top: "45%", left: "55%", barrierFree: true },
    { id: 2, name: "방배2동 주민센터", type: "호우", distance: "2.5km", capacity: "150명", top: "30%", left: "30%", barrierFree: false },
    { id: 3, name: "남부교회 대피소", type: "지진", distance: "800m", capacity: "300명", top: "65%", left: "25%", barrierFree: true },
    { id: 4, name: "잠원초등학교", type: "대설", distance: "3.1km", capacity: "800명", top: "70%", left: "70%", barrierFree: false },
    { id: 5, name: "반포종합운동장", type: "전체", distance: "1.5km", capacity: "2000명", top: "20%", left: "65%", barrierFree: true },
  ];

  const selectedData = selectedMarker ? markers.find(m => m.id === selectedMarker) : null;

  return (
    <div className="flex-1 flex flex-col animate-in fade-in duration-500 relative -mx-5 -mb-24 mt-[-12px]">
      
      {/* Top Floating Controls (iOS Maps Style) */}
      <div className="absolute top-0 left-0 w-full z-20 pt-[24px] px-4 flex flex-col gap-3 pointer-events-none">
        
        {/* Search Bar */}
        <div className="bg-white/80 backdrop-blur-xl rounded-[16px] px-4 py-3 shadow-[0_2px_12px_rgba(0,0,0,0.06)] border border-black/5 flex items-center gap-3 pointer-events-auto">
          <Search size={20} className="text-[#8E8E93]" strokeWidth={2} />
          <span className="text-[16px] font-medium text-[#8E8E93] flex-1 tracking-tight">장소 또는 주소 검색</span>
        </div>

        {/* Categories / Filters */}
        <div className="flex gap-2 overflow-x-auto hide-scrollbar pointer-events-auto pb-2 -mx-4 px-4">
          <button
            onClick={() => setFilterBarrierFree(!filterBarrierFree)}
            className={cn(
              "flex items-center gap-1.5 px-4 py-2 rounded-full text-[14px] font-semibold whitespace-nowrap transition-all border",
              filterBarrierFree 
                ? "bg-[#0A84FF] text-white border-[#0A84FF]" 
                : "bg-white/90 backdrop-blur-md text-[#111111] border-black/5 shadow-sm"
            )}
          >
            <Accessibility size={16} strokeWidth={2.5} />
            배리어프리
          </button>
          
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={cn(
                "px-4 py-2 rounded-full text-[14px] font-semibold whitespace-nowrap transition-all border",
                activeCategory === cat
                  ? "bg-[#111111] text-white border-[#111111]"
                  : "bg-white/90 backdrop-blur-md text-[#111111] border-black/5 shadow-sm"
              )}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* The Map Canvas (Apple Maps Vibe) */}
      <div className="flex-1 bg-[#F9F9FB] relative overflow-hidden" onClick={() => setSelectedMarker(null)}>
        {/* Abstract Minimalist Map */}
        <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg" className="absolute inset-0">
          <defs>
            <pattern id="grid" width="30" height="30" patternUnits="userSpaceOnUse">
              <path d="M 30 0 L 0 0 0 30" fill="none" stroke="#E5E5EA" strokeWidth="0.5" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
          
          {/* Subtle River */}
          <path d="M -50 400 Q 150 450 450 350 L 450 430 Q 150 530 -50 480 Z" fill="#E6F0F9" />
          
          {/* Subtle Parks */}
          <path d="M 250 150 Q 300 100 350 150 T 300 250 Z" fill="#E8F4E6" />
          <path d="M 50 600 Q 100 550 150 650 T 0 700 Z" fill="#E8F4E6" />

          {/* Clean Roads */}
          <path d="M -20 200 Q 200 150 450 100" fill="none" stroke="#FFFFFF" strokeWidth="8" strokeLinecap="round" />
          <path d="M 150 -20 L 200 300 L 100 850" fill="none" stroke="#FFFFFF" strokeWidth="12" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M -20 650 L 250 550 L 450 650" fill="none" stroke="#FFFFFF" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>

        {/* Markers */}
        {markers.map(marker => {
          if (filterBarrierFree && !marker.barrierFree) return null;
          if (activeCategory !== "전체" && marker.type !== activeCategory && activeCategory !== "병원") return null;
          
          const isSelected = selectedMarker === marker.id;
          
          return (
            <div 
              key={marker.id}
              onClick={(e) => {
                e.stopPropagation();
                setSelectedMarker(marker.id);
              }}
              className={cn(
                "absolute transform -translate-x-1/2 -translate-y-full cursor-pointer group",
                isSelected ? "z-30" : "z-10"
              )}
              style={{ top: marker.top, left: marker.left }}
            >
              <div className="flex flex-col items-center relative">
                {/* Marker Pin */}
                <div className={cn(
                  "w-9 h-9 rounded-full flex items-center justify-center shadow-[0_4px_12px_rgba(0,0,0,0.1)] border-[2px] transition-all duration-300 relative",
                  isSelected 
                    ? "bg-[#111111] border-white text-white scale-[1.15]"
                    : marker.barrierFree 
                      ? "bg-[#0A84FF] border-white text-white" 
                      : "bg-white border-white text-[#111111]"
                )}>
                  {marker.barrierFree ? <Accessibility size={18} strokeWidth={2.5} /> : <MapPin size={18} strokeWidth={2.5} />}
                </div>

                {/* Text Label */}
                <div className={cn(
                  "absolute top-11 bg-white/90 backdrop-blur-xl px-3 py-1.5 rounded-[10px] shadow-[0_2px_8px_rgba(0,0,0,0.06)] text-[13px] font-semibold whitespace-nowrap border transition-all duration-300",
                  isSelected ? "border-black/10 text-[#111111] scale-100 opacity-100 translate-y-0" : "border-transparent text-[#8E8E93] opacity-0 translate-y-1 pointer-events-none group-hover:opacity-100 group-hover:translate-y-0"
                )}>
                  {marker.name}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      
      {/* Right Floating FABs */}
      <div className={cn(
        "absolute right-4 z-20 flex flex-col gap-3 pointer-events-auto transition-all duration-500",
        selectedData ? "bottom-[280px]" : "bottom-32"
      )}>
        <button className="w-[44px] h-[44px] bg-white/90 backdrop-blur-xl rounded-full shadow-[0_4px_16px_rgba(0,0,0,0.08)] border border-black/5 flex items-center justify-center text-[#111111] active:bg-[#F2F2F7] transition-all">
          <Layers size={20} strokeWidth={2} />
        </button>
        <button className="w-[44px] h-[44px] bg-white/90 backdrop-blur-xl rounded-full shadow-[0_4px_16px_rgba(0,0,0,0.08)] border border-black/5 flex items-center justify-center text-[#0A84FF] active:bg-[#F2F2F7] transition-all">
          <LocateFixed size={20} strokeWidth={2} />
        </button>
      </div>

      {/* Bottom Sheet for Selected Marker */}
      <div className={cn(
        "absolute left-0 w-full z-30 px-3 transition-all duration-500 ease-[cubic-bezier(0.32,0.72,0,1)]",
        selectedData ? "bottom-24 opacity-100 translate-y-0" : "bottom-0 opacity-0 translate-y-12 pointer-events-none"
      )}>
        {selectedData && (
          <div className="bg-white rounded-[24px] p-5 shadow-[0_16px_40px_rgba(0,0,0,0.12)] border border-black/5 pointer-events-auto relative">
            {/* Drag Handle */}
            <div className="absolute top-2 left-1/2 -translate-x-1/2 w-10 h-1.5 bg-[#E5E5EA] rounded-full"></div>
            
            <button 
              onClick={() => setSelectedMarker(null)}
              className="absolute top-4 right-4 p-1.5 bg-[#F2F2F7] rounded-full text-[#8E8E93] hover:bg-[#E5E5EA] transition-colors"
            >
              <X size={18} strokeWidth={2.5} />
            </button>
            
            <div className="flex items-start gap-4 mt-2 mb-6">
              <div className={cn(
                "w-12 h-12 rounded-[14px] flex items-center justify-center text-white shadow-sm",
                selectedData.barrierFree ? "bg-[#0A84FF]" : "bg-[#111111]"
              )}>
                {selectedData.barrierFree ? <Accessibility size={24} strokeWidth={2.5} /> : <MapPin size={24} strokeWidth={2.5} />}
              </div>
              <div className="flex-1 pt-0.5">
                <h3 className="font-bold text-[20px] text-[#111111] mb-1 tracking-tight">{selectedData.name}</h3>
                <div className="flex flex-wrap items-center gap-1.5 text-[14px] font-medium text-[#8E8E93]">
                  <span className="text-[#0A84FF]">{selectedData.distance}</span>
                  <span>•</span>
                  <span>{selectedData.type}</span>
                  <span>•</span>
                  <span>수용 {selectedData.capacity}</span>
                </div>
              </div>
            </div>
            
            <div className="flex gap-3">
              <button className="flex-[0.4] bg-[#F2F2F7] text-[#111111] font-semibold text-[15px] py-3.5 rounded-[16px] flex items-center justify-center gap-2 active:bg-[#E5E5EA] transition-colors">
                상세 정보
              </button>
              <button className="flex-[0.6] bg-[#0A84FF] text-white font-semibold text-[15px] py-3.5 rounded-[16px] flex items-center justify-center gap-2 shadow-[0_4px_12px_rgba(10,132,255,0.3)] active:bg-[#0070E0] transition-all">
                <Navigation size={18} strokeWidth={2.5} /> 경로 안내
              </button>
            </div>
          </div>
        )}
      </div>

    </div>
  );
}

