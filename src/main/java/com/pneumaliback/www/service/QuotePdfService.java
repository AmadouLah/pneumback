package com.pneumaliback.www.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;
import com.pneumaliback.www.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    public byte[] generateQuote(QuoteRequest request, User emitter) {
        if (request == null) {
            log.error("La demande de devis est null");
            throw new IllegalArgumentException("La demande de devis ne peut pas être null");
        }

        try {
            log.debug("Génération du PDF pour le devis ID: {}, RequestNumber: {}", request.getId(),
                    request.getRequestNumber());
            String htmlContent = buildHtmlTemplate(request, emitter);

            if (htmlContent == null || htmlContent.isBlank()) {
                log.error("Le contenu HTML généré est vide pour le devis {}", request.getId());
                throw new IllegalStateException("Le contenu HTML généré est vide");
            }

            log.debug("Contenu HTML généré, longueur: {} caractères", htmlContent.length());
            byte[] pdf = renderPdfFromHtml(htmlContent);
            log.debug("PDF généré avec succès, taille: {} bytes", pdf != null ? pdf.length : 0);
            return pdf;
        } catch (IOException e) {
            log.error("Erreur I/O lors de la génération du PDF du devis", e);
            throw new IllegalStateException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF du devis: {}", e.getMessage(), e);
            throw new IllegalStateException("Impossible de générer le PDF du devis: " + e.getMessage(), e);
        }
    }

    private byte[] renderPdfFromHtml(String htmlContent) throws IOException {
        if (htmlContent == null || htmlContent.isBlank()) {
            log.error("Le contenu HTML est vide, impossible de générer le PDF");
            throw new IllegalStateException("Le contenu HTML est vide");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, null);
            builder.useFastMode();
            builder.toStream(outputStream);
            builder.run();

            byte[] pdfBytes = outputStream.toByteArray();
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("Le PDF généré est vide");
                throw new IllegalStateException("Le PDF généré est vide");
            }

            if (pdfBytes.length < 100) {
                log.warn("Le PDF généré est très petit ({} bytes), il pourrait être invalide", pdfBytes.length);
            }

            log.debug("PDF généré avec succès, taille: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Erreur lors du rendu PDF: {}", e.getMessage(), e);
            throw new IOException("Erreur lors du rendu PDF: " + e.getMessage(), e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn("Erreur lors de la fermeture du stream: {}", e.getMessage());
            }
        }
    }

    private String buildHtmlTemplate(QuoteRequest request, User emitter) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='fr'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8' />");
        html.append("<style>").append(getCssStyles()).append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append(buildHeader(emitter));

        // Quote Info
        html.append(buildQuoteInfo(request));

        // Client Info
        html.append(buildClientInfo(request));

        // Items Table
        html.append(buildItemsTable(request));

        // Totals
        html.append(buildTotals(request));

        // Notes
        html.append(buildNotes(request));

        // Footer
        html.append(buildFooter());

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String getCssStyles() {
        return """
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    font-size: 12px;
                    color: #333;
                    line-height: 1.6;
                    padding: 20px;
                }
                .header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 30px;
                    padding-bottom: 20px;
                    border-bottom: 3px solid #2563eb;
                }
                .logo-section {
                    flex: 1;
                }
                .company-name {
                    font-size: 24px;
                    font-weight: bold;
                    color: #2563eb;
                    margin-bottom: 5px;
                }
                .company-details {
                    font-size: 10px;
                    color: #666;
                }
                .quote-badge {
                    background: #2563eb;
                    color: white;
                    padding: 10px 20px;
                    border-radius: 5px;
                    font-size: 14px;
                    font-weight: bold;
                }
                .quote-info {
                    background: #f8fafc;
                    padding: 15px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                }
                .quote-info-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 8px;
                }
                .quote-info-label {
                    font-weight: bold;
                    color: #666;
                }
                .quote-info-value {
                    color: #333;
                }
                .section {
                    margin-bottom: 25px;
                }
                .section-title {
                    font-size: 14px;
                    font-weight: bold;
                    color: #2563eb;
                    margin-bottom: 10px;
                    padding-bottom: 5px;
                    border-bottom: 2px solid #e5e7eb;
                }
                .client-info {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 20px;
                    margin-bottom: 20px;
                }
                .info-block {
                    background: #f8fafc;
                    padding: 12px;
                    border-radius: 5px;
                }
                .info-block-label {
                    font-size: 10px;
                    color: #666;
                    text-transform: uppercase;
                    margin-bottom: 5px;
                }
                .info-block-value {
                    font-size: 12px;
                    color: #333;
                    font-weight: 500;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 20px;
                }
                thead {
                    background: #2563eb;
                    color: white;
                }
                th {
                    padding: 12px;
                    text-align: left;
                    font-size: 11px;
                    font-weight: bold;
                    text-transform: uppercase;
                }
                td {
                    padding: 10px 12px;
                    border-bottom: 1px solid #e5e7eb;
                    font-size: 11px;
                }
                tbody tr:hover {
                    background: #f8fafc;
                }
                .text-right {
                    text-align: right;
                }
                .text-center {
                    text-align: center;
                }
                .totals-section {
                    margin-top: 20px;
                    margin-left: auto;
                    width: 300px;
                }
                .total-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 8px 0;
                    border-bottom: 1px solid #e5e7eb;
                }
                .total-row:last-child {
                    border-bottom: none;
                }
                .total-label {
                    font-weight: 500;
                    color: #666;
                }
                .total-value {
                    font-weight: bold;
                    color: #333;
                }
                .total-final {
                    font-size: 16px;
                    color: #2563eb;
                    padding-top: 10px;
                    margin-top: 10px;
                    border-top: 2px solid #2563eb;
                }
                .notes-section {
                    background: #f8fafc;
                    padding: 15px;
                    border-radius: 5px;
                    margin-top: 20px;
                }
                .notes-title {
                    font-weight: bold;
                    color: #2563eb;
                    margin-bottom: 8px;
                }
                .notes-content {
                    color: #666;
                    font-size: 11px;
                    line-height: 1.8;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 2px solid #e5e7eb;
                    text-align: center;
                    color: #666;
                    font-size: 10px;
                }
                """;
    }

    private String buildHeader(User emitter) {
        StringBuilder header = new StringBuilder();
        header.append("<div class='header'>");
        header.append("<div class='logo-section'>");
        header.append("<div class='company-name'>PneuMali</div>");
        header.append("<div class='company-details'>");

        if (emitter != null) {
            header.append(escapeHtml(safe(emitter.getFullName()))).append("<br />");
            header.append(escapeHtml(safe(emitter.getEmail()))).append("<br />");
            header.append(escapeHtml(safe(emitter.getPhoneNumber())));
        } else {
            header.append("Landouré Amadou<br />");
            header.append("amadoulandoure004@gmail.com<br />");
            header.append("+223 70 91 11 12");
        }

        Address address = resolvePreferredAddress(emitter);
        if (address != null) {
            header.append("<br />").append(escapeHtml(formatAddress(address)));
        } else {
            header.append("<br />Bamako, Mali");
        }

        header.append("</div>");
        header.append("</div>");
        header.append("<div class='quote-badge'>DEVIS</div>");
        header.append("</div>");
        return header.toString();
    }

    private String buildQuoteInfo(QuoteRequest request) {
        StringBuilder info = new StringBuilder();
        info.append("<div class='quote-info'>");
        info.append("<div class='quote-info-row'>");
        info.append("<span class='quote-info-label'>Numéro de demande:</span>");
        info.append("<span class='quote-info-value'>").append(escapeHtml(request.getRequestNumber())).append("</span>");
        info.append("</div>");

        if (request.getQuoteNumber() != null && !request.getQuoteNumber().isBlank()) {
            info.append("<div class='quote-info-row'>");
            info.append("<span class='quote-info-label'>Numéro de devis:</span>");
            info.append("<span class='quote-info-value'>").append(escapeHtml(request.getQuoteNumber()))
                    .append("</span>");
            info.append("</div>");
        }

        info.append("<div class='quote-info-row'>");
        info.append("<span class='quote-info-label'>Date de création:</span>");
        info.append("<span class='quote-info-value'>").append(DATE_FORMAT.format(request.getCreatedAt().toLocalDate()))
                .append("</span>");
        info.append("</div>");

        if (request.getValidUntil() != null) {
            info.append("<div class='quote-info-row'>");
            info.append("<span class='quote-info-label'>Valide jusqu'au:</span>");
            info.append("<span class='quote-info-value'>").append(DATE_FORMAT.format(request.getValidUntil()))
                    .append("</span>");
            info.append("</div>");
        }

        info.append("</div>");
        return info.toString();
    }

    private String buildClientInfo(QuoteRequest request) {
        StringBuilder client = new StringBuilder();
        client.append("<div class='section'>");
        client.append("<div class='section-title'>Informations Client</div>");
        client.append("<div class='client-info'>");

        User user = request.getUser();
        Address address = resolvePreferredAddress(user);

        client.append("<div class='info-block'>");
        client.append("<div class='info-block-label'>Nom</div>");
        client.append("<div class='info-block-value'>").append(escapeHtml(user.getFullName())).append("</div>");
        client.append("</div>");

        client.append("<div class='info-block'>");
        client.append("<div class='info-block-label'>Email</div>");
        client.append("<div class='info-block-value'>").append(escapeHtml(user.getEmail())).append("</div>");
        client.append("</div>");

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            client.append("<div class='info-block'>");
            client.append("<div class='info-block-label'>Téléphone</div>");
            client.append("<div class='info-block-value'>").append(escapeHtml(user.getPhoneNumber())).append("</div>");
            client.append("</div>");
        }

        client.append("<div class='info-block'>");
        client.append("<div class='info-block-label'>Adresse</div>");
        client.append("<div class='info-block-value'>");
        if (address != null) {
            client.append(escapeHtml(formatAddress(address)));
        } else {
            client.append("Non communiqué");
        }
        client.append("</div>");
        client.append("</div>");

        client.append("</div>");
        client.append("</div>");
        return client.toString();
    }

    private String buildItemsTable(QuoteRequest request) {
        StringBuilder table = new StringBuilder();
        table.append("<div class='section'>");
        table.append("<div class='section-title'>Détails des Articles</div>");
        table.append("<table>");
        table.append("<thead>");
        table.append("<tr>");
        table.append("<th>Description</th>");
        table.append("<th class='text-center'>Qté</th>");
        table.append("<th class='text-right'>Prix unitaire</th>");
        table.append("<th class='text-right'>Total</th>");
        table.append("</tr>");
        table.append("</thead>");
        table.append("<tbody>");

        List<QuoteRequestItem> items = null;
        try {
            items = request.getItems();
            if (items != null) {
                log.debug("Génération du tableau avec {} articles pour le devis {}", items.size(), request.getId());
            } else {
                log.warn("La liste d'articles est null pour le devis {}", request.getId());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.error("Les items du devis {} ne sont pas chargés: {}", request.getId(), e.getMessage());
            items = null;
        }

        if (items == null || items.isEmpty()) {
            log.warn("Aucun article trouvé pour le devis {}", request.getId());
            table.append(
                    "<tr><td colspan='4' style='text-align: center; padding: 20px; color: #999;'>Aucun article dans ce devis</td></tr>");
        } else {
            for (QuoteRequestItem item : items) {
                if (item == null) {
                    continue;
                }
                table.append("<tr>");
                table.append("<td>");
                table.append("<strong>").append(escapeHtml(safe(item.getProductName()))).append("</strong>");
                if (item.getBrandName() != null && !item.getBrandName().isBlank()) {
                    table.append("<br /><span style='color: #666; font-size: 10px;'>")
                            .append(escapeHtml(item.getBrandName()))
                            .append("</span>");
                }
                if (item.getWidthValue() != null && item.getProfileValue() != null && item.getDiameterValue() != null) {
                    table.append("<br /><span style='color: #666; font-size: 10px;'>")
                            .append(item.getWidthValue())
                            .append("/")
                            .append(item.getProfileValue())
                            .append(" R")
                            .append(item.getDiameterValue())
                            .append("</span>");
                }
                table.append("</td>");
                table.append("<td class='text-center'>").append(item.getQuantity() != null ? item.getQuantity() : 0)
                        .append("</td>");
                table.append("<td class='text-right'>").append(formatCurrency(item.getUnitPrice())).append("</td>");
                BigDecimal lineTotal = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                table.append("<td class='text-right'><strong>").append(formatCurrency(lineTotal))
                        .append("</strong></td>");
                table.append("</tr>");
            }
        }

        table.append("</tbody>");
        table.append("</table>");
        table.append("</div>");
        return table.toString();
    }

    private String buildTotals(QuoteRequest request) {
        StringBuilder totals = new StringBuilder();
        totals.append("<div class='totals-section'>");

        totals.append("<div class='total-row'>");
        totals.append("<span class='total-label'>Sous-total:</span>");
        totals.append("<span class='total-value'>").append(formatCurrency(request.getSubtotalRequested()))
                .append("</span>");
        totals.append("</div>");

        if (request.getDiscountTotal() != null && request.getDiscountTotal().compareTo(BigDecimal.ZERO) > 0) {
            totals.append("<div class='total-row'>");
            totals.append("<span class='total-label'>Remise:</span>");
            totals.append("<span class='total-value'>-").append(formatCurrency(request.getDiscountTotal()))
                    .append("</span>");
            totals.append("</div>");
        }

        BigDecimal total = request.getTotalQuoted() != null
                ? request.getTotalQuoted()
                : request.getSubtotalRequested();

        totals.append("<div class='total-row total-final'>");
        totals.append("<span class='total-label'>TOTAL:</span>");
        totals.append("<span class='total-value'>").append(formatCurrency(total)).append("</span>");
        totals.append("</div>");

        totals.append("</div>");
        return totals.toString();
    }

    private String buildNotes(QuoteRequest request) {
        StringBuilder notes = new StringBuilder();

        boolean hasNotes = (request.getAdminNotes() != null && !request.getAdminNotes().isBlank())
                || (request.getDeliveryDetails() != null && !request.getDeliveryDetails().isBlank());

        if (hasNotes) {
            notes.append("<div class='notes-section'>");

            if (request.getAdminNotes() != null && !request.getAdminNotes().isBlank()) {
                notes.append("<div class='notes-title'>Notes administratives:</div>");
                notes.append("<div class='notes-content'>").append(escapeHtml(request.getAdminNotes()))
                        .append("</div>");
            }

            if (request.getDeliveryDetails() != null && !request.getDeliveryDetails().isBlank()) {
                String marginTop = request.getAdminNotes() != null && !request.getAdminNotes().isBlank() 
                        ? " style='margin-top: 15px;'" 
                        : "";
                notes.append("<div class='notes-title'").append(marginTop).append(">Détails de livraison:</div>");
                notes.append("<div class='notes-content'>").append(escapeHtml(request.getDeliveryDetails()))
                        .append("</div>");
            }

            notes.append("</div>");
        }

        return notes.toString();
    }

    private String buildFooter() {
        return "<div class='footer'>" +
                "Merci de votre confiance. Pour toute question, n'hésitez pas à nous contacter.<br />" +
                "© " + java.time.Year.now() + " PneuMali - Tous droits réservés" +
                "</div>";
    }

    private Address resolvePreferredAddress(User user) {
        if (user == null) {
            return null;
        }
        try {
            if (user.getAddresses() == null || user.getAddresses().isEmpty()) {
                return null;
            }
            return user.getAddresses().stream()
                    .filter(Objects::nonNull)
                    .filter(Address::isDefault)
                    .findFirst()
                    .orElse(user.getAddresses().stream().filter(Objects::nonNull).findFirst().orElse(null));
        } catch (org.hibernate.LazyInitializationException e) {
            log.debug("Les adresses de l'utilisateur ne sont pas chargées, utilisation de l'adresse par défaut");
            return null;
        }
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "";
        }
        StringBuilder formatted = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            formatted.append(address.getStreet());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            appendPart(formatted, address.getCity());
        }
        if (address.getRegion() != null && !address.getRegion().isBlank()) {
            appendPart(formatted, address.getRegion());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            appendPart(formatted, address.getPostalCode());
        }
        if (address.getCountry() != null) {
            appendPart(formatted, address.getCountry().getDisplayName());
        }
        return formatted.toString().trim();
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(part.trim());
    }

    private String safe(String value) {
        return value != null ? value : "Non communiqué";
    }

    private String formatCurrency(BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        return String.format(Locale.FRENCH, "%,.2f FCFA", safeValue);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
