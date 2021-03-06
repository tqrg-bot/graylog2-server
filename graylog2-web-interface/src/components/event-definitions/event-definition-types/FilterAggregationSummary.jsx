import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import AggregationExpressionParser from 'logic/alerts/AggregationExpressionParser';
import PermissionsMixin from 'util/PermissionsMixin';

import { TIME_UNITS } from './FilterForm';

class FilterAggregationSummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  getConditionType = (config) => {
    const { group_by: groupBy, series, conditions } = config;
    return (lodash.isEmpty(groupBy)
    && (!conditions || lodash.isEmpty(conditions) || conditions.expression === null)
    && lodash.isEmpty(series)
      ? 'filter' : 'aggregation');
  };

  render() {
    const { config, currentUser } = this.props;
    const {
      query,
      streams,
      search_within_ms: searchWithinMs,
      execute_every_ms: executeEveryMs,
      group_by: groupBy,
      series,
      conditions,
    } = config;

    const conditionType = this.getConditionType(config);

    const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
    const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

    const expressionResults = AggregationExpressionParser.parseExpression(conditions);

    const effectiveStreams = PermissionsMixin.isPermitted(currentUser.permissions, 'streams:read')
      ? streams : [];

    return (
      <dl>
        <dt>Type</dt>
        <dd>{lodash.upperFirst(conditionType)}</dd>
        <dt>Search Query</dt>
        <dd>{query || '*'}</dd>
        <dt>Streams</dt>
        <dd>{effectiveStreams && effectiveStreams.length > 0 ? effectiveStreams.join(', ') : 'No streams selected'}</dd>
        <dt>Search within</dt>
        <dd>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</dd>
        <dt>Execute search every</dt>
        <dd>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</dd>
        {conditionType === 'aggregation' && (
          <React.Fragment>
            <dt>Group by Field(s)</dt>
            <dd>{groupBy && groupBy.length > 0 ? groupBy.join(', ') : 'No Group by configured'}</dd>
            <dt>Create Events if</dt>
            <dd>
              {series[0] ? <em>{series[0].function}({series[0].field})</em> : <span>No series selected</span>}
              {' '}{expressionResults.operator} {expressionResults.value}
            </dd>
          </React.Fragment>
        )}
      </dl>
    );
  }
}

export default FilterAggregationSummary;
