"use client";

import React from "react";

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");

const renderInlineMarkdown = (value: string) => {
  let html = escapeHtml(value);

  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_match, alt, src) => {
    const isItemIcon = src.includes("/img/item/");
    const className = isItemIcon
      ? "mx-1 my-2 inline-block h-12 w-12 rounded-xl border border-[#ffd1e3] bg-[#fff0f7] object-cover align-middle shadow-[0_8px_16px_rgba(205,79,134,0.12)]"
      : "my-4 max-h-[28rem] w-full rounded-[1.5rem] border border-[#ffd1e3] object-cover shadow-[0_18px_38px_rgba(205,79,134,0.12)]";

    return `<img src="${src}" alt="${alt}" loading="lazy" decoding="async" class="${className}" />`;
  });
  html = html.replace(
    /\[([^\]]+)\]\(([^)]+)\)/g,
    '<a href="$2" target="_blank" rel="noopener noreferrer" class="font-black text-[#e75491] underline decoration-[#ffc1d8] underline-offset-4">$1</a>'
  );
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  html = html.replace(/\*([^*]+)\*/g, "<em>$1</em>");
  html = html.replace(
    /`([^`]+)`/g,
    '<code class="rounded-md bg-[#fff0f7] px-1.5 py-0.5 text-[#d94683]">$1</code>'
  );

  return html;
};

const markdownToHtml = (markdown: string) => {
  const lines = markdown.replace(/\r\n/g, "\n").split("\n");
  const blocks: string[] = [];
  let listItems: string[] = [];
  let orderedListItems: string[] = [];
  let quoteLines: string[] = [];
  let codeLines: string[] = [];
  let isCodeBlock = false;

  const flushList = () => {
    if (listItems.length > 0) {
      blocks.push(`<ul>${listItems.join("")}</ul>`);
      listItems = [];
    }
    if (orderedListItems.length > 0) {
      blocks.push(`<ol>${orderedListItems.join("")}</ol>`);
      orderedListItems = [];
    }
  };

  const flushQuote = () => {
    if (quoteLines.length > 0) {
      blocks.push(`<blockquote>${quoteLines.join("<br />")}</blockquote>`);
      quoteLines = [];
    }
  };

  lines.forEach((line) => {
    if (line.trim().startsWith("```")) {
      if (isCodeBlock) {
        blocks.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
        codeLines = [];
        isCodeBlock = false;
      } else {
        flushList();
        flushQuote();
        isCodeBlock = true;
      }
      return;
    }

    if (isCodeBlock) {
      codeLines.push(line);
      return;
    }

    if (line.trim() === "") {
      flushList();
      flushQuote();
      return;
    }

    const headingMatch = /^(#{1,3})\s+(.+)$/.exec(line);
    if (headingMatch) {
      flushList();
      flushQuote();
      const level = headingMatch[1].length;
      blocks.push(`<h${level}>${renderInlineMarkdown(headingMatch[2])}</h${level}>`);
      return;
    }

    if (/^[-*]\s+/.test(line)) {
      flushQuote();
      if (orderedListItems.length > 0) {
        blocks.push(`<ol>${orderedListItems.join("")}</ol>`);
        orderedListItems = [];
      }
      listItems.push(`<li>${renderInlineMarkdown(line.replace(/^[-*]\s+/, ""))}</li>`);
      return;
    }

    if (/^\d+\.\s+/.test(line)) {
      flushQuote();
      if (listItems.length > 0) {
        blocks.push(`<ul>${listItems.join("")}</ul>`);
        listItems = [];
      }
      orderedListItems.push(
        `<li>${renderInlineMarkdown(line.replace(/^\d+\.\s+/, ""))}</li>`
      );
      return;
    }

    if (line.startsWith(">")) {
      flushList();
      quoteLines.push(renderInlineMarkdown(line.replace(/^>\s?/, "")));
      return;
    }

    flushList();
    flushQuote();
    blocks.push(`<p>${renderInlineMarkdown(line)}</p>`);
  });

  flushList();
  flushQuote();

  if (isCodeBlock && codeLines.length > 0) {
    blocks.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
  }

  return blocks.join("");
};

interface MarkdownPreviewProps {
  markdown: string;
  emptyLabel?: string;
}

export default function MarkdownPreview({
  markdown,
  emptyLabel = "미리볼 내용이 없습니다.",
}: MarkdownPreviewProps) {
  if (!markdown.trim()) {
    return (
      <div className="flex min-h-[14rem] items-center justify-center rounded-[1.5rem] bg-[#fff0f7] text-sm font-bold text-[#a76886]">
        {emptyLabel}
      </div>
    );
  }

  return (
    <div
      className="guide-markdown rounded-[1.5rem] bg-white/82 p-5 text-[#69324b] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.8)]"
      dangerouslySetInnerHTML={{ __html: markdownToHtml(markdown) }}
    />
  );
}
