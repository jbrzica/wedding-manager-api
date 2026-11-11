package hr.jbrzica.wedding_manager_api.ugovor;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popunjava Word predložak: svaka {{oznaka}} zamijeni se vrijednošću, a oblikovanje ostaje.
 *
 * Word često razbije tekst na više dijelova (runova), npr. "{{ime" i "}}" u dva runa kad se
 * oznaka upisivala u dva navrata ili ju je provjera pravopisa podcrtala. Zato se oznaka traži
 * u tekstu cijelog odlomka, a zamjena se upiše u prvi run koji ju dodiruje.
 */
public final class WordPredlozak {

    private static final Pattern OZNAKA = Pattern.compile("\\{\\{\\s*([a-z_]+)\\s*}}");

    private WordPredlozak() {
    }

    public static byte[] popuni(byte[] predlozak, Map<String, String> vrijednosti) throws IOException {
        try (XWPFDocument dokument = new XWPFDocument(new ByteArrayInputStream(predlozak));
             ByteArrayOutputStream izlaz = new ByteArrayOutputStream()) {
            popuniElemente(dokument.getBodyElements(), vrijednosti);
            for (XWPFHeader zaglavlje : dokument.getHeaderList()) {
                popuniElemente(zaglavlje.getBodyElements(), vrijednosti);
            }
            for (XWPFFooter podnozje : dokument.getFooterList()) {
                popuniElemente(podnozje.getBodyElements(), vrijednosti);
            }
            dokument.write(izlaz);
            return izlaz.toByteArray();
        }
    }

    private static void popuniElemente(List<IBodyElement> elementi, Map<String, String> vrijednosti) {
        for (IBodyElement element : elementi) {
            if (element instanceof XWPFParagraph odlomak) {
                popuniOdlomak(odlomak, vrijednosti);
            } else if (element instanceof XWPFTable tablica) {
                for (XWPFTableRow red : tablica.getRows()) {
                    for (XWPFTableCell celija : red.getTableCells()) {
                        popuniElemente(celija.getBodyElements(), vrijednosti);
                    }
                }
            }
        }
    }

    static void popuniOdlomak(XWPFParagraph odlomak, Map<String, String> vrijednosti) {
        int odakle = 0;
        while (true) {
            List<XWPFRun> runovi = odlomak.getRuns();
            StringBuilder cijeli = new StringBuilder();
            for (XWPFRun run : runovi) {
                cijeli.append(tekst(run));
            }
            Matcher m = OZNAKA.matcher(cijeli);
            if (!m.find(odakle)) {
                return;
            }
            String vrijednost = vrijednosti.get(m.group(1));
            if (vrijednost == null) {
                // Nepoznata oznaka ostaje vidljiva u ugovoru, da se primijeti tipfeler u predlošku
                odakle = m.end();
                continue;
            }
            zamijeni(runovi, m.start(), m.end(), vrijednost);
            odakle = m.start() + vrijednost.length();
        }
    }

    // Tekst od pocetak do kraj (u tekstu cijelog odlomka) zamijeni vrijednošću
    private static void zamijeni(List<XWPFRun> runovi, int pocetak, int kraj, String vrijednost) {
        int pozicija = 0;
        boolean upisano = false;
        for (XWPFRun run : runovi) {
            String t = tekst(run);
            int od = pozicija;
            int doPoz = pozicija + t.length();
            pozicija = doPoz;
            if (doPoz <= pocetak || od >= kraj) {
                continue;
            }
            int lokalniPocetak = Math.max(pocetak - od, 0);
            int lokalniKraj = Math.min(kraj - od, t.length());
            String prije = t.substring(0, lokalniPocetak);
            String poslije = t.substring(lokalniKraj);
            if (!upisano) {
                postavi(run, prije + vrijednost + poslije);
                upisano = true;
            } else {
                postavi(run, prije + poslije);
            }
        }
    }

    private static String tekst(XWPFRun run) {
        StringBuilder sb = new StringBuilder();
        for (CTText t : run.getCTR().getTList()) {
            sb.append(t.getStringValue());
        }
        return sb.toString();
    }

    private static void postavi(XWPFRun run, String tekst) {
        CTR ctr = run.getCTR();
        while (ctr.sizeOfTArray() > 0) {
            ctr.removeT(0);
        }
        if (!tekst.isEmpty()) {
            CTText t = ctr.addNewT();
            t.setStringValue(tekst);
            // Bez ovoga Word pojede razmake na početku i kraju
            t.setSpace(org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute.Space.PRESERVE);
        }
    }
}
