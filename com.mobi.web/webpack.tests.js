import 'angular-mocks';

var testsContext = require.context('./src/main/resources/public', true, /spec\.js$/);
testsContext.keys().forEach(testsContext);