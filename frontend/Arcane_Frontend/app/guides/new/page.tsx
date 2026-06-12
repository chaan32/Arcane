"use client";

import { ImagePlus, Loader2, Send } from "lucide-react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { ChangeEvent, useMemo, useState } from "react";
import ChampionGuidePicker from "@/components/guides/ChampionGuidePicker";
import ItemGuidePicker, {
  getGuideItemMarkdown,
  type GuideItem,
} from "@/components/guides/ItemGuidePicker";
import MarkdownPreview from "@/components/guides/MarkdownPreview";
import { useMockAuth } from "@/hooks/useMockAuth";
import type { GuideChampion } from "@/types/community";
import LoginDialog from "@/components/auth/LoginDialog";
import { createGuidePost } from "@/services/communityApi";

const readFileAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

export default function NewGuidePage() {
  const router = useRouter();
  const { user } = useMockAuth();
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [selectedChampion, setSelectedChampion] =
    useState<GuideChampion | null>(null);
  const [markdown, setMarkdown] = useState(
    "## 핵심 운영\n\n- 라인전에서 가장 중요한 포인트\n- 첫 귀환 이후 아이템 선택\n\n## 콤보\n\n`Q` 이후 평타를 섞어주세요.\n\n## 주의할 점\n\n상대 정글 위치를 확인하기 전에는 무리하지 않습니다."
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  const canSubmit = useMemo(
    () =>
      Boolean(
        user &&
          selectedChampion &&
          title.trim().length >= 2 &&
          markdown.trim().length >= 10 &&
          !isSubmitting
      ),
    [isSubmitting, markdown, selectedChampion, title, user]
  );

  const handleImageUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    if (files.length === 0) return;

    const nextImageUrls = await Promise.all(files.map(readFileAsDataUrl));
    setMarkdown((currentMarkdown) =>
      [
        currentMarkdown,
        "",
        ...nextImageUrls.map(
          (imageUrl, index) => `![공략 이미지 ${index + 1}](${imageUrl})`
        ),
      ].join("\n")
    );
    event.target.value = "";
  };

  const handleItemInsert = (item: GuideItem) => {
    setMarkdown((currentMarkdown) =>
      [currentMarkdown, "", getGuideItemMarkdown(item)].join("\n")
    );
  };

  const handleSubmit = async () => {
    if (!canSubmit || !user || !selectedChampion) return;

    setIsSubmitting(true);
    setMessage("공략을 등록하는 중입니다.");

    try {
      const guide = await createGuidePost({
        title: title.trim(),
        championId: selectedChampion.id,
        markdown:
          summary.trim().length > 0
            ? [`> ${summary.trim()}`, "", markdown].join("\n")
            : markdown,
      });

      router.push(`/guides/${guide.id}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "공략 등록에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-12 text-[#69324b]">
        <div className="mx-auto max-w-[34rem] rounded-[2rem] border border-white/75 bg-white/94 p-8 text-center shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
          <Image
            src="/sad_mumu.png"
            alt="로그인 안내"
            width={144}
            height={144}
            className="mx-auto h-36 w-36 object-contain"
          />
          <h1 className="mt-4 text-2xl font-black">로그인이 필요합니다</h1>
          <div className="mt-6 flex justify-center gap-2">
            <button
              type="button"
              onClick={() => setIsLoginOpen(true)}
              className="rounded-full bg-[#f45f9c] px-5 py-3 text-sm font-black text-white"
            >
              로그인 방법 선택
            </button>
          </div>
        </div>
        <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-8 text-[#69324b]">
      <div className="mx-auto grid w-full max-w-[90rem] gap-5 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
        <section className="rounded-[2rem] border border-white/75 bg-white/94 p-5 shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
          <div className="mb-5">
            <div className="mb-2 inline-flex rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
              Markdown Editor
            </div>
            <h1 className="text-3xl font-black">공략 작성</h1>
          </div>

          <div className="flex flex-col gap-4">
            <ChampionGuidePicker
              selectedChampion={selectedChampion}
              onSelect={setSelectedChampion}
            />
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="제목"
              className="h-14 rounded-[1.25rem] border border-[#ffd1e3] bg-[#fffafd] px-4 text-lg font-black text-[#69324b] outline-none placeholder:text-[#bd7b98]"
            />
            <input
              value={summary}
              onChange={(event) => setSummary(event.target.value)}
              placeholder="요약"
              className="h-12 rounded-[1.25rem] border border-[#ffd1e3] bg-[#fffafd] px-4 text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
            />
            <textarea
              value={markdown}
              onChange={(event) => setMarkdown(event.target.value)}
              className="min-h-[30rem] resize-y rounded-[1.5rem] border border-[#ffd1e3] bg-[#fffafd] p-4 font-mono text-sm leading-7 text-[#69324b] outline-none placeholder:text-[#bd7b98]"
            />
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-2">
                <label className="inline-flex h-12 cursor-pointer items-center gap-2 rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-5 text-sm font-black text-[#e75491] transition-colors hover:bg-[#ffe0ee]">
                  <ImagePlus className="h-4 w-4" />
                  이미지 추가
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={handleImageUpload}
                    className="hidden"
                  />
                </label>
                <ItemGuidePicker onSelect={handleItemInsert} />
              </div>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={!canSubmit}
                className="inline-flex h-12 items-center gap-2 rounded-full bg-[#f45f9c] px-6 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491] disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-[#f4b6cf]"
              >
                {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                {isSubmitting ? "등록 중" : "등록"}
              </button>
            </div>
            {message && (
              <p className="text-sm font-bold text-[#a76886]">{message}</p>
            )}
          </div>
        </section>

        <section className="rounded-[2rem] border border-white/75 bg-white/94 p-5 shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-black">미리보기</h2>
            {selectedChampion && (
              <span className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491]">
                {selectedChampion.nameKo}
              </span>
            )}
          </div>
          <MarkdownPreview markdown={markdown} />
        </section>
      </div>
    </div>
  );
}
