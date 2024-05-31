package org.soc.sonices;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.JsonData;
import jakarta.json.JsonString;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GcOverheadSketch {

    private static final Pattern GC_OVERHEAD = Pattern.compile("spent \\[([0-9]*\\.?[0-9]*)(ms|s)\\]");

    public static SearchResponse<Void> query(ESClient client, long start, long now) throws IOException {
        Query query = BoolQuery.of(
            m -> m.filter(
                MatchPhraseQuery.of(tq -> tq.field("event.dataset").query("elasticsearch.log"))._toQuery(),
                MatchPhraseQuery.of(tq -> tq.field("log.logger").query("org.elasticsearch.monitor.jvm.JvmGcMonitorService"))._toQuery(),
                MatchPhraseQuery.of(tq -> tq.field("message").query("overhead"))._toQuery(),
                RangeQuery.of(rq -> rq.field("@timestamp").format("epoch_millis").gte(JsonData.of(start)).lte(JsonData.of(now)))._toQuery()
            )
        )._toQuery();
        return client.getClient()
            .search(
                b -> b.index("serverless-logging-*:logs-elasticsearch*")
                    .size(500)
                    .source(SourceConfig.of(scf -> scf.fetch(false)))
                    .query(query)
                    .fields(
                        FieldAndFormat.of(f -> f.field("message")),
                        FieldAndFormat.of(f2 -> f2.field("@timestamp").format("epoch_millis"))
                    ),
                Void.class
            );
    }

    public static void parseAndPlay(SearchResponse<Void> response, long start, Sequence sq) throws InvalidMidiDataException {
        Track track = sq.createTrack();
        for (Hit hit : response.hits().hits()) {
            Map<String, JsonData> fields = hit.fields();
            String message = ((JsonString) fields.get("message").toJson().asJsonArray().get(0)).getString();
            String timestamp = ((JsonString) fields.get("@timestamp").toJson().asJsonArray().get(0)).getString();
            Matcher matcher = GC_OVERHEAD.matcher(message);
            matcher.find();
            String timeValue = matcher.group(1);
            String timeUnit = matcher.group(2);
            long duration = timeUnit.equals("ms") ? Long.parseLong(timeValue) : (long) (1000 * Double.parseDouble(timeValue));
            long tick = Long.parseLong(timestamp) - start;
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 2, 65, 120), tick));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 2, 65, 120), tick + duration));
        }
    }
}
