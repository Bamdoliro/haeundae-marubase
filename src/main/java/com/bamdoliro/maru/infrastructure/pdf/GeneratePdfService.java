package com.bamdoliro.maru.infrastructure.pdf;

import com.bamdoliro.maru.infrastructure.pdf.exception.FailedToExportPdfException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.layout.font.FontProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * HTML을 PDF로 변환한다.
 * FontProvider는 변환 중 만든 PdfFont를 캐싱하고 PdfFont는 특정 PdfDocument에 종속되므로,
 * FontProvider와 ConverterProperties를 여러 변환이 함께 쓰면 다른 문서의 폰트 객체를 참조해
 * "Pdf indirect object belongs to other PDF document" 에러가 발생한다(동시 요청 시 재현됨).
 * 그래서 폰트 파일은 서비스 생성 시 한 번만 읽어 FontProgram으로 캐싱하고,
 * FontProvider와 ConverterProperties는 변환할 때마다 새로 만든다.
 */
@Service
public class GeneratePdfService {

    private static final String FONT_PATH = new ClassPathResource("/static/fonts").getPath();
    private static final List<String> FONTS = List.of(
            "/SUIT-Bold.ttf",
            "/SUIT-SemiBold.ttf",
            "/SUIT-Medium.ttf",
            "/SUIT-Regular.ttf",
            "/SUIT-Light.ttf"
    );

    private final List<FontProgram> fontPrograms = loadFontPrograms();
    private final String baseUri = resolveBaseUri();

    public ByteArrayOutputStream execute(String html) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream, createConverterProperties());
        return outputStream;
    }

    private ConverterProperties createConverterProperties() {
        FontProvider fontProvider = new DefaultFontProvider(false, false, false);
        fontPrograms.forEach(fontProvider::addFont);

        ConverterProperties properties = new ConverterProperties();
        properties.setFontProvider(fontProvider);
        properties.setBaseUri(baseUri);
        return properties;
    }

    private static List<FontProgram> loadFontPrograms() {
        return FONTS.stream()
                .map(font -> {
                    try {
                        return FontProgramFactory.createFont(FONT_PATH + font);
                    } catch (IOException e) {
                        throw new FailedToExportPdfException();
                    }
                })
                .toList();
    }

    private static String resolveBaseUri() {
        try {
            return new ClassPathResource("/static/").getURL().toExternalForm();
        } catch (IOException e) {
            throw new FailedToExportPdfException();
        }
    }
}
