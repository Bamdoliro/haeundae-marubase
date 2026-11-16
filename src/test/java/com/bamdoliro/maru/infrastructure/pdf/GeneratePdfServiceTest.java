package com.bamdoliro.maru.infrastructure.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratePdfServiceTest {

    private static final String HTML = """
            <html>
            <head><style>body { font-family: SUIT; }</style></head>
            <body>
              <p style="font-weight: 700">자기소개서</p>
              <p style="font-weight: 600">학업계획서</p>
              <p style="font-weight: 500">수험번호 111001</p>
              <p style="font-weight: 400">홍길동</p>
              <p style="font-weight: 300">해운대고등학교</p>
            </body>
            </html>
            """;

    private final GeneratePdfService generatePdfService = new GeneratePdfService();

    @Test
    void 여러_건을_연속으로_변환해도_매번_정상적인_PDF가_만들어진다() {
        for (int i = 0; i < 30; i++) {
            assertEquals(1, pages(generatePdfService.execute(HTML)));
        }
    }

    @Test
    void 여러_요청이_동시에_변환해도_다른_문서의_폰트를_참조하지_않는다() throws Exception {
        int threads = 4;
        int conversionsPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Integer>> jobs = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                jobs.add(() -> {
                    int total = 0;
                    for (int i = 0; i < conversionsPerThread; i++) {
                        total += pages(generatePdfService.execute(HTML));
                    }
                    return total;
                });
            }

            for (Future<Integer> result : pool.invokeAll(jobs)) {
                assertEquals(conversionsPerThread, result.get());
            }
        } finally {
            pool.shutdown();
        }
    }

    private static int pages(ByteArrayOutputStream pdf) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf.toByteArray())))) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
