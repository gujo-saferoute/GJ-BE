import { Outlet, NavLink } from "react-router";
import { Home, Map, Settings } from "lucide-react";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function Layout() {
  return (
    <div className="bg-[#E5E5EA] min-h-screen flex items-center justify-center p-4 sm:p-8 font-sans antialiased text-[#111111]">
      <div className="bg-[#F2F2F7] w-full max-w-[400px] h-[850px] max-h-screen rounded-[40px] shadow-2xl overflow-hidden flex flex-col relative ring-8 ring-white/50">
        {/* Dynamic Island / Status Bar spacer */}
        <div className="h-12 w-full flex justify-center items-start pt-3 z-50 pointer-events-none">
          <div className="w-[120px] h-7 bg-black rounded-full shadow-sm" />
        </div>

        {/* Scrollable Content */}
        <main className="flex-1 flex flex-col overflow-y-auto pb-24 px-5 hide-scrollbar relative">
          <Outlet />
        </main>

        {/* Bottom Tab Bar */}
        <nav className="absolute bottom-0 w-full bg-white/80 backdrop-blur-xl border-t border-black/5 px-6 pb-8 pt-3 flex justify-between items-center z-50">
          <NavLink
            to="/"
            className={({ isActive }) =>
              cn(
                "flex flex-col items-center gap-1 w-16 transition-colors",
                isActive ? "text-[#111111]" : "text-[#8E8E93]"
              )
            }
          >
            <Home size={24} strokeWidth={isActive => isActive ? 2.5 : 2} />
            <span className="text-[10px] font-semibold tracking-wide">홈</span>
          </NavLink>
          <NavLink
            to="/map"
            className={({ isActive }) =>
              cn(
                "flex flex-col items-center gap-1 w-16 transition-colors",
                isActive ? "text-[#111111]" : "text-[#8E8E93]"
              )
            }
          >
            <Map size={24} strokeWidth={isActive => isActive ? 2.5 : 2} />
            <span className="text-[10px] font-semibold tracking-wide">지도</span>
          </NavLink>
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              cn(
                "flex flex-col items-center gap-1 w-16 transition-colors",
                isActive ? "text-[#111111]" : "text-[#8E8E93]"
              )
            }
          >
            <Settings size={24} strokeWidth={isActive => isActive ? 2.5 : 2} />
            <span className="text-[10px] font-semibold tracking-wide">설정</span>
          </NavLink>
        </nav>
      </div>

      <style>{`
        .hide-scrollbar::-webkit-scrollbar {
          display: none;
        }
        .hide-scrollbar {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}</style>
    </div>
  );
}
