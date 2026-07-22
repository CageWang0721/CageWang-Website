package com.example.blog.article.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    private static final Pattern LATIN_WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create()
    );
    private static final Set<String> ALLOWED_CONTENT_CLASSES = Set.of(
            "wc-font--sans", "wc-font--serif", "wc-font--mono",
            "wc-size--small", "wc-size--normal", "wc-size--large", "wc-size--xlarge",
            "wc-color--accent", "wc-color--red", "wc-color--blue", "wc-color--green", "wc-color--muted",
            "wc-highlight--yellow", "wc-highlight--green", "wc-highlight--pink",
            "wc-align--left", "wc-align--center", "wc-align--right",
            "wc-image", "wc-image--left", "wc-image--center", "wc-image--right",
            "wc-image--small", "wc-image--medium", "wc-image--large", "wc-image--full"
    );

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer articleRenderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            // The editor emits a small set of presentation tags. Jsoup still sanitizes
            // every rendered result and the class allowlist below removes unknown styles.
            .escapeHtml(false)
            .build();
    private final HtmlRenderer commentRenderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            // Public comments support Markdown, but raw HTML is always displayed as text.
            .escapeHtml(true)
            .build();
    private final Safelist articleSafelist = baseSafelist()
            .addTags("figure", "figcaption", "mark", "u", "sup", "sub")
            .addAttributes(":all", "class")
            .addAttributes("figure", "class");
    private final Safelist commentSafelist = baseSafelist();

    public RenderedMarkdown renderArticle(String markdown) {
        return render(markdown, articleRenderer, articleSafelist, true);
    }

    public RenderedMarkdown renderComment(String markdown) {
        return render(markdown, commentRenderer, commentSafelist, false);
    }

    private RenderedMarkdown render(
            String markdown,
            HtmlRenderer renderer,
            Safelist safelist,
            boolean retainArticleClasses
    ) {
        String source = markdown == null ? "" : markdown;
        String rawHtml = renderer.render(parser.parse(source));
        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleanHtml = Jsoup.clean(rawHtml, "", safelist, outputSettings);
        Document document = Jsoup.parseBodyFragment(cleanHtml);
        if (retainArticleClasses) {
            retainAllowedArticleClasses(document);
        }
        for (Element link : document.select("a[href]")) {
            if (link.hasAttr("href") && isExternal(link.attr("href"))) {
                link.attr("rel", "noopener noreferrer nofollow");
            }
        }
        String html = document.body().html();
        String plain = document.text();
        int wordCount = countWords(plain);
        int readingMinutes = wordCount == 0 ? 0 : Math.max(1, (int) Math.ceil(wordCount / 300.0));
        return new RenderedMarkdown(html, plain, wordCount, readingMinutes);
    }

    private static Safelist baseSafelist() {
        return Safelist.relaxed()
                .addTags("table", "thead", "tbody", "tfoot", "tr", "th", "td", "del")
                .addAttributes("th", "align")
                .addAttributes("td", "align")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("a", "href", "http", "https", "mailto");
    }

    private void retainAllowedArticleClasses(Document document) {
        for (Element styled : document.select("[class]")) {
            Set<String> classes = new LinkedHashSet<>(styled.classNames());
            classes.retainAll(ALLOWED_CONTENT_CLASSES);
            if (classes.isEmpty()) {
                styled.removeAttr("class");
            } else {
                styled.classNames(classes);
            }
        }
    }

    private boolean isExternal(String href) {
        return href.startsWith("http://") || href.startsWith("https://");
    }

    private int countWords(String text) {
        int cjkCount = 0;
        StringBuilder nonCjk = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                cjkCount++;
                nonCjk.append(' ');
            } else {
                nonCjk.appendCodePoint(codePoint);
            }
        }
        long latinCount = LATIN_WORD.matcher(nonCjk).results().count();
        return Math.toIntExact(cjkCount + latinCount);
    }

    public record RenderedMarkdown(
            String html,
            String plain,
            int wordCount,
            int readingMinutes
    ) {
    }
}
