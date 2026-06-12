import { Star } from "@/components/icons/Star";
import { SkillTooltip } from "@/components/common/SkillTooltip";
import {
  getDataDragonChampionIconUrl,
  getDataDragonPassiveIconUrl,
  getDataDragonSpellIconUrl,
} from "@/services/dataDragonApi";
import type { ChampionInfo } from "@/types/champion";
import Image from "next/image";

interface ChampionProfileProps {
  championNameEn: string;
  championName: string;
  championDisplayName: string;
  skill?: ChampionInfo;
}

// 역할군 영문을 한글로 변환 _ api에서 영어로 내려주고 있음
const translateTag = (tag: string): string => {
  const tagMap: Record<string, string> = {
    Mage: "마법사",
    Fighter: "전사",
    Tank: "탱커",
    Assassin: "암살자",
    Marksman: "원거리",
    Support: "서포터",
  };
  return tagMap[tag] || tag;
};

export function ChampionProfile({
  championNameEn,
  championName,
  championDisplayName,
  skill,
}: ChampionProfileProps) {
  return (
    <div className="flex min-w-0 flex-row items-center gap-[1.5rem]">
      <div className="relative h-[9rem] w-[9rem] shrink-0 overflow-hidden rounded-[1.25rem] border border-[#ffc4dc] bg-[#fff0f7] shadow-[0_16px_34px_rgba(244,114,182,0.18)]">
        <Image
          src={getDataDragonChampionIconUrl(
            skill?.imageFull || `${championNameEn}.png`,
            skill?.version
          )}
          alt={championDisplayName}
          width={128}
          height={128}
          sizes="128px"
          className="w-full h-full object-cover"
        />
      </div>

      <div className="flex min-w-0 flex-col">
        <div className="mb-[1rem] flex flex-col gap-[0.45rem]">
          {skill?.tags && skill.tags.length > 0 && (
            <div className="flex flex-row items-center gap-[0.4rem]">
              {skill.tags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full border border-[#ffd1e3] bg-[#fff7fb] px-[0.65rem] py-[0.2rem] text-[0.82rem] font-black text-[#a76886]"
                >
                  {translateTag(tag)}
                </span>
              ))}
            </div>
          )}
          <div className="flex min-w-0 flex-row items-center gap-[0.5rem]">
            <h1 className="truncate text-[3rem] font-black leading-none tracking-normal text-[#69324b]">
              {championName}
            </h1>
            <button className="flex h-[2.25rem] w-[2.25rem] shrink-0 items-center justify-center rounded-full border border-[#ffd1e3] bg-white text-text-default transition hover:bg-[#fff0f7]">
              <Star className="h-[1.45rem] w-[1.45rem]" />
            </button>
          </div>
        </div>

        <div className="flex gap-[0.4rem]">
          {skill?.passive && (
            <SkillTooltip
              title={`패시브: ${skill.passive.name}`}
              description={skill.passive.description}
            >
              <Image
                src={getDataDragonPassiveIconUrl(skill.passive.imageFull, skill.version)}
                className="h-[3rem] w-[3rem] rounded-[0.5rem] border border-[#ffd1e3] cursor-help"
                alt="P"
                width={48}
                height={48}
                sizes="48px"
              />
            </SkillTooltip>
          )}
          {skill?.spells?.slice(0, 4).map((spell, index) => (
            <SkillTooltip
              key={index}
              title={`${spell.spellKey}: ${spell.name}`}
              description={spell.description}
              additionalInfo={`쿨다운: ${spell.cooldown} | 코스트: ${spell.cost}`}
            >
              <Image
                src={getDataDragonSpellIconUrl(spell.imageFull, spell.version || skill.version)}
                className="h-[3rem] w-[3rem] rounded-[0.5rem] border border-[#ffd1e3] cursor-help"
                alt={spell.spellKey}
                width={48}
                height={48}
                sizes="48px"
              />
            </SkillTooltip>
          ))}
        </div>
      </div>
    </div>
  );
}
