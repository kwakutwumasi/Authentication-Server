import React, {Component} from 'react';

import Paper from  '@material-ui/core/Paper';
import Grid from "@material-ui/core/Grid";

import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableRow from "@material-ui/core/TableRow";

import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";

import PlusIcon from '@material-ui/icons/ExposurePlus1';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import CircularProgress from "@material-ui/core/CircularProgress";
import CloseIcon from '@material-ui/icons/Close';
import IconButton from "@material-ui/core/IconButton";

import Card from "@material-ui/core/Card";
import CardContent from "@material-ui/core/CardContent";
import CardActions from "@material-ui/core/CardActions";

class DeviceForm extends Component {
  constructor(props){
    super(props);
    const managementRequest = {
      authorizationRequest:{deviceId:"",otp:""},
      requests:[],
    };

    const managementResponse = {
      responses:[]
    }

    props.devices.forEach((device,index)=>{
      if(device.checked){
        device.requestResult="";
        if(props.showAlias && props.showDeviceId){
          managementRequest.requests.push({deviceId:device.id, alias:device.aliases.pop()});
        } else if (props.showDeviceId) {
          managementRequest.requests.push({deviceId:device.id});
        } else {
          managementRequest.requests.push({alias:device.aliases.pop()});
        }
      }
    });

    this.state = {
      managementRequest:managementRequest,
      managementResponse:managementResponse,
      loading:false,
      showAuthorization:false
    }
  }

  requestValueChanged = (event, field, id) => {
    const managementRequest = this.state.managementRequest;
    managementRequest.requests[id][field] = event.target.value;
    this.setState({managementRequest:managementRequest});
  }

  authorizationValueChanged = (event, field) => {
    const managementRequest = this.state.managementRequest;
    managementRequest.authorizationRequest[field] = event.target.value;
    this.setState({managementRequest:managementRequest});
  }

  addRequest = (event) => {
    const managementRequest = this.state.managementRequest;
    const props = this.props;
    if(props.showAlias && props.showDeviceId){
      managementRequest.requests.push({deviceId:"", alias:""});
    } else if (props.showDeviceId) {
      managementRequest.requests.push({deviceId:""});
    } else {
      managementRequest.requests.push({alias:""});
    }
    this.setState({managementRequest:managementRequest});
  }

  executeRequest = (event) => {
    const managementResponse = {
      responses:[]
    }
    this.setState({loading:true, showAuthorization:false});
    const $this = this;
    window.setTimeout(function(){
      $this.state.managementRequest.requests.forEach((request,index)=>{
          managementResponse.responses.push({message:"Test message",error:!request.deviceId || request.deviceId.startsWith("1")});
      });
      $this.setState({managementResponse:managementResponse, loading:false});
    }, 1500);
  }

  requestAuthorization = (event) =>{
    this.setState({showAuthorization:true});
  }

  handleClose = (event) =>{
    this.setState({showAuthorization:false});
  }

  render(){
    const {classes, aliasFieldName, showDeviceId, showAlias, requestType} = this.props;
    return (
      <Grid container spacing={1}>
        <Grid item xs={false} sm={false} md={false} lg={2} />
        <Grid item xs={12} sm={12} md={12} lg={8}>
          <Paper className={classes.formPaper}>
            <Table className={classes.table}>
              <TableBody>
                  <TableRow>
                    <TableCell variant="head" align="left" colSpan={2}><h3>{requestType} Management Requests</h3></TableCell>
                  </TableRow>
                  {this.state.managementRequest.requests.map((request, index) => {
                    return <TableRow key={index} className={classes.tableRow}>
                            <TableCell className={!showDeviceId && classes.hiddenCell} variant="body" align="left">
                              <TextField label="Device ID"
                                fullWidth={true}
                                value={request.deviceId}
                                className={classes.textField}
                                placeholder="234253425634567"
                                margin="normal"
                                onChange={(event)=>this.requestValueChanged(event,"deviceId",index)}
                                error={!this.props.showAlias
                                    && this.state.managementResponse.responses[index]
                                    && this.state.managementResponse.responses[index].error}
                                helperText={!this.props.showAlias
                                    && this.state.managementResponse.responses[index]
                                    && this.state.managementResponse.responses[index].message?
                                  this.state.managementResponse.responses[index].message:""}
                                FormHelperTextProps ={{
                                  className:classes.successMessage
                                }} />
                            </TableCell>
                            <TableCell className={!showAlias && classes.hiddenCell} variant="body" align="left">
                              <TextField label={aliasFieldName}
                                fullWidth={true}
                                value={request.alias}
                                className={classes.textField}
                                placeholder="name@server.com"
                                margin="normal"
                                onChange={(event)=>this.requestValueChanged(event,"alias",index)}
                                error={this.props.showAlias
                                    && this.state.managementResponse.responses[index]
                                    && this.state.managementResponse.responses[index].error}
                                helperText={this.props.showAlias
                                    && this.state.managementResponse.responses[index]
                                    && this.state.managementResponse.responses[index].message?
                                  this.state.managementResponse.responses[index].message:""}
                                FormHelperTextProps ={{
                                    className:classes.successMessage
                                }} />
                            </TableCell>
                          </TableRow>;
                  })}
              </TableBody>
              <Button variant="contained" color="default" className={classes.button}
                onClick={this.addRequest} aria-label="Edit">
                <PlusIcon /> {this.state.managementRequest.requests.length>0?"More":"Add a Request"}
              </Button>
              <Button variant="contained" color="default" className={classes.button}
                onClick={this.requestAuthorization} aria-label="Edit">
                {this.state.loading? <CircularProgress size={24} className={classes.buttonProgress} />:<KeyboardReturnIcon />} Execute {requestType} Request
              </Button>
            </Table>
          </Paper>
          {this.state.showAuthorization && <div>
            <div className={classes.backgroundOverlay}>
            </div>
            <Grid container spacing={1}>
              <Grid item xs={false} sm={3} md={2} lg={4} />
              <Grid item xs={12} sm={6} md={8} lg={4}>
              <Card className={classes.cardOverlay}>
                <CardContent>
                	<IconButton
            					key="close"
            					aria-label="Close"
            					color="inherit"
            					className={classes.cardClose}
            					onClick={this.handleClose}>
            					<CloseIcon />
            				</IconButton>
                    <h3>Authorization</h3>
                    <p className={classes.instructionsSmall}>* Note: The currently signed in Administrator cannot authorize this request</p>
                  <TextField label="Administrator Device ID/Alias"
                    fullWidth={true}
                    value={this.state.managementRequest.authorizationRequest.deviceId}
                    className={classes.textField}
                    placeholder="123456789012345"
                    margin="normal"
                    required={true}
                    onChange={event => this.authorizationValueChanged(event,"deviceId")} />
                  <TextField label="TOTP Code"
                    fullWidth={true}
                    value={this.state.managementRequest.authorizationRequest.otp}
                    className={classes.textField}
                    margin="normal"
                    required={true}
                    type="password"
                    onChange={event => this.authorizationValueChanged(event,"otp")} />
                </CardContent>
                <CardActions>
                  <Button variant="contained" color="default" className={classes.button}
                    onClick={this.executeRequest} aria-label="Edit">
                    {this.state.loading? <CircularProgress size={24} className={classes.buttonProgress} />:<KeyboardReturnIcon />} Execute {requestType} Request
                  </Button>
                </CardActions>
              </Card>
              </Grid>
            </Grid>
          </div>}
        </Grid>
      </Grid>
    );
  }
}

export default DeviceForm;
