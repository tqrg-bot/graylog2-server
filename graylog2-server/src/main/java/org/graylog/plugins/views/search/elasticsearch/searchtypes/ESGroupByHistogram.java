/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import com.google.common.collect.ImmutableMap;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.GroupBy;
import org.graylog.plugins.views.search.searchtypes.GroupByHistogram;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog.plugins.views.search.elasticsearch.searchtypes.IntervalHelper.createDefaultInterval;

public class ESGroupByHistogram implements ESSearchTypeHandler<GroupByHistogram>  {
    private GroupBy createGroupBy(GroupByHistogram groupByHistogram) {
        return GroupBy.builder()
                .id(groupByHistogram.id())
                .fields(groupByHistogram.fields())
                .limit(groupByHistogram.limit())
                .operation(groupByHistogram.operation())
                .order(groupByHistogram.order())
                .build();
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, GroupByHistogram groupByHistogram, ESGeneratedQueryContext queryContext) {
        final String mainField = groupByHistogram.fields().get(0);
        final List<String> stackedFields = groupByHistogram.fields().subList(1, groupByHistogram.fields().size());
        final Searches.DateHistogramInterval interval = firstNonNull(groupByHistogram.interval(), createDefaultInterval(query.timerange()));

        final GroupBy groupBy = createGroupBy(groupByHistogram);
        final ESGroupBy esGroupBy = new ESGroupBy();

        final DateHistogramAggregationBuilder histogram = AggregationBuilders.dateHistogram(histogramAggName(groupByHistogram))
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(interval.toESInterval())
                .subAggregation(esGroupBy.createTermsBuilder(mainField, stackedFields, groupBy));

        queryContext.searchSourceBuilder(groupByHistogram).aggregation(histogram);
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, GroupByHistogram groupByHistogram, SearchResult queryResult, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final DateHistogramAggregation aggregation = aggregations.getDateHistogramAggregation(histogramAggName(groupByHistogram));
        final GroupBy groupBy = createGroupBy(groupByHistogram);
        final ESGroupBy esGroupBy = new ESGroupBy();
        final ImmutableMap.Builder<Long, GroupByHistogram.Bucket> buckets = ImmutableMap.builder();

        for (DateHistogramAggregation.DateHistogram dateHistogram : aggregation.getBuckets()) {
            final DateTime keyAsDate = new DateTime(dateHistogram.getKey());
            final TermsAggregation termsAggregation = dateHistogram.getFilterAggregation(esGroupBy.filterAggName(groupBy))
                    .getTermsAggregation(esGroupBy.termsAggName(groupBy));

            final GroupBy.Result groupByResult = esGroupBy.extractTermsAggregationResult(groupBy, termsAggregation);

            buckets.put(keyAsDate.getMillis(), GroupByHistogram.Bucket.builder()
                    .groups(groupByResult.groups())
                    .build());
        }

        return GroupByHistogram.Result.builder()
                .id(groupByHistogram.id())
                .buckets(buckets.build())
                .build();
    }

    private String histogramAggName(GroupByHistogram groupByHistogram) {
        return String.format(Locale.ENGLISH, "group-by-histogram-%s", groupByHistogram.id());
    }
}
