import React, { Component } from 'react';
import {Route, Link, withRouter} from "react-router-dom";
import axios from "axios"

import {AppBar,Toolbar,Typography} from "@material-ui/core";
import { withStyles } from "@material-ui/core/styles";
import PropTypes from "prop-types";
import green from "@material-ui/core/colors/green";

import TextField from "@material-ui/core/TextField";

import Drawer from "@material-ui/core/Drawer";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";

import Divider from "@material-ui/core/Divider";
import Grid from "@material-ui/core/Grid";

import HomeIcon from "@material-ui/icons/Home";
import ArrowForwardIosIcon from "@material-ui/icons/ArrowForwardIos";
import AddAliasSVG from "./images/addalias.svg"
import CloseIcon from '@material-ui/icons/Close';
import RemoveAliasSVG from "./images/removealias.svg"
import LockDeviceSVG from "./images/lockdevice.svg"
import UnLockDeviceSVG from "./images/unlockdevice.svg"
import DeactivateDeviceSVG from "./images/deactivatedevice.svg"
import GroupAddIcon from '@material-ui/icons/GroupAdd';
import PermIdentityIcon from '@material-ui/icons/PermIdentity';
import MenuIcon from "@material-ui/icons/Menu";
import IconButton from "@material-ui/core/IconButton";

import Snackbar from '@material-ui/core/Snackbar';
import Card from "@material-ui/core/Card";
import CardContent from "@material-ui/core/CardContent";
import CardActions from "@material-ui/core/CardActions";

import HomePage from "./components/HomePage.js"
import DeviceForm from "./components/DeviceFormComponent.js"

const styles = theme =>({
  centered:{
    textAlign:"center"
  },
  toolbar: theme.mixins.toolbar,
  close: {
    padding: theme.spacing.unit / 2,
  },
  menuButton: {
    marginLeft: -12,
    marginRight: 20,
  },
  textField: {
    marginLeft: theme.spacing.unit,
    marginRight: theme.spacing.unit,
    minWidth: 300,
    display: "inline-block"
  },
  searchField: {
    marginLeft: theme.spacing.unit,
    marginRight: theme.spacing.unit,
    maxWidth: 300,
    display: "inline-block"
  },
  card: {
    marginTop: "5.0em",
    marginLeft: "auto",
    marginRight: "auto",
    opacity: 1.0,
    backgroundColor:"white"
  },
  cardOverlay:{
    position: "fixed",
    marginTop: "5.0em",
    marginLeft: "auto",
    marginRight: "auto",
  	top: 0,
  	bottom: 0,
  	left: 0,
  	right: 0,
  	display: "block",
  	zIndex: 1002,
  	textAlign: "center",
    maxWidth:500,
    height:350,
    backgroundColor:"white"
  },
  backgroundOverlay:{
    position: "fixed",
  	top: 0,
  	bottom: 0,
  	left: 0,
  	right: 0,
  	backgroundColor: "#1f1f1f",
  	opacity: 0.7,
  	display: "block",
  	zIndex: 1001,
  	textAlign: "center"
  },
  homePaperFirst:{
      marginTop:theme.spacing.unit * 10,
      marginLeft:theme.spacing.unit * 4,
      marginRight:theme.spacing.unit * 4,
      padding: theme.spacing.unit * 2
  },
  homePaper:{
      marginTop:theme.spacing.unit * 3,
      marginLeft:theme.spacing.unit * 4,
      marginRight:theme.spacing.unit * 4,
      padding: theme.spacing.unit * 2
  },
  formPaper:{
      marginTop:theme.spacing.unit * 10,
      marginLeft:theme.spacing.unit * 1,
      marginRight:theme.spacing.unit * 1,
      padding: theme.spacing.unit * 2
  },
  table: {
     whiteSpace:"nowrap"
  },
  tableRow:{
    height:48
  },
  tableActions:{
    height:"3.0em"
  },
  paginationRight:{
    float:"right"
  },
  tableCellHiddenSm:{
    [theme.breakpoints.down('sm')]: {
      display:"none"
    }
  },
  hiddenCell:{
    display:"none"
  },
  button:{
    marginTop:theme.spacing.unit,
    marginRight:theme.spacing.unit
  },
  buttonProgress: {
     color: green[500],
     position:"relative",
     right:5
  },
  successMessage:{
    color: green[500]
  },
  cardClose:{
    position:"absolute",
    right:5
  },
  instructionsSmall:{
    fontSize:10,
    fontWeight:"bold"
  }
});

const apibase = "";
const iconHeight = 30;

const alldevices = [{id:"1234534534641",aliases:[],status:"ACTIVE", itemCount:1},
{id:"45263563475672",aliases:["test3"],status:"INACTIVE", itemCount:2},
{id:"76574453245233",aliases:["test4","test5","test6"],status:"LOCKED", itemCount:3},
{id:"56354347564554",aliases:["test7"],status:"INITIATED", itemCount:4},
{id:"12345345346455",aliases:["test1","test2"],status:"ACTIVE", itemCount:5},
{id:"45263563475676",aliases:["test3"],status:"INACTIVE", itemCount:6},
{id:"76574453245237",aliases:["test4","test5","test6"],status:"LOCKED", itemCount:7},
{id:"56354347564558",aliases:["test7"],status:"INITIATED", itemCount:8},
{id:"12345345346499",aliases:["test1","test2"],status:"ACTIVE", itemCount:9},
{id:"45263563475670",aliases:["test3"],status:"INACTIVE", itemCount:10},
{id:"76574453245231",aliases:["test4","test5","test6"],status:"LOCKED", itemCount:11},
{id:"56354347564552",aliases:["test7"],status:"INITIATED", itemCount:12}];

class App extends Component {
  constructor(props){
    super(props);
    this.state = {
      state:{drawerOpen:false},
      error:false,
      errorMessage:"",
      signedin:true,
      status:"",
      maxrows:5,
      devices:[],
      lastId:0,
      previousLastId:[],
      previousActive:false,
      nextActive:false,
      allChecked:false,
      deviceId:"",
      otp:"",
      loading:false,
      administrators:[]
    }
  }

  handleDrawerToggle=()=>{
      this.setState( state=>({ drawerOpen: !state.drawerOpen }));
  };

  valueChanged=(event,field)=>{
    this.setState({[field]:event.target.value});
  }

  signin=(event)=>{
    this.setState({signedin:true});
  }

  next=(event)=>{
    if(this.state.lastId<alldevices.length){
      var devicespart = alldevices.slice(this.state.lastId, this.state.lastId+this.state.maxrows);
      this.state.previousLastId.push(this.state.lastId);
      this.setState({devices:devicespart, allChecked:false, lastId:devicespart[devicespart.length-1].itemCount, previousActive:true});
    } else {
      this.setState({nextActive:false});
    }
  }

  previous=(event)=>{
    if(this.state.previousLastId.length>0){
      var previousLastId = this.state.previousLastId.pop();
      if(previousLastId>=this.state.maxrows){
        var devicespart = alldevices.slice(previousLastId-this.state.maxrows, previousLastId);
        this.setState({devices:devicespart, allChecked:false, lastId:devicespart[devicespart.length-1].itemCount, nextActive:true});
      }
    } else {
      this.setState({previousActive:false});
    }
  }

  search=(event)=>{
    var $this = this;
    this.setState({loading:true});
    window.setTimeout(function(){
      var devicespart = alldevices.slice(0,$this.state.maxrows);
      $this.setState({devices:devicespart, allChecked:false, lastId:devicespart[devicespart.length-1].itemCount, nextActive:true, loading:false});
    }, 1500);
  }

  allCheckboxChanged=()=>{
    var currentState = !this.state.allChecked;
    var devices = this.state.devices;
    devices.forEach(function(element,index){
      element.allChecked = currentState;
    });
    this.setState({allChecked:currentState, devices:devices});
  }

  itemCheck=(id, checked)=>{
    this.state.devices.forEach(function(element,index){
      if(element.id === id){
        element.checked = checked;
      }
    });
  }

  handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    const {payload} = this.state;
    payload.action = "no";
    const data = JSON.stringify(payload);
    this.websocket.current.sendMessage(data);
    this.setState({showMessage:false});
  }

  render() {
    const { classes } = this.props;
    if(this.state.signedin){
    return (
      <div>
        <AppBar position="fixed">
          <Toolbar>
            <IconButton className={classes.menuButton}
              onClick={this.handleDrawerToggle}
              color="inherit" aria-label="Menu">
              <MenuIcon />
            </IconButton>
            <Typography variant="title" color="inherit">
              TOTP Server Management Application
            </Typography>
          </Toolbar>
        </AppBar>
        <nav>
          <Drawer open={this.state.drawerOpen}
            onClick={this.handleDrawerToggle}
            variant="temporary">
            <div>
              <div className={classes.toolbar} />
              <Divider />
              <List>
                <ListItem button component={Link} to="/">
                  <ListItemIcon><HomeIcon /></ListItemIcon>
                  <ListItemText>Home</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/assign">
                  <ListItemIcon><img src={AddAliasSVG} alt="Assign Aliases" height={iconHeight} /></ListItemIcon>
                  <ListItemText>Assign Aliases</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/unassign">
                  <ListItemIcon><img src={RemoveAliasSVG} alt="Un-assign Aliases" height={iconHeight} /></ListItemIcon>
                  <ListItemText>Un-assign Aliases</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/lock">
                  <ListItemIcon><img src={LockDeviceSVG} alt="Lock Devices" height={iconHeight} /></ListItemIcon>
                  <ListItemText>Lock Devices</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/unlock">
                  <ListItemIcon><img src={UnLockDeviceSVG} alt="Un-lock Devices" height={iconHeight} /></ListItemIcon>
                  <ListItemText>Un-lock Devices</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/deactivate">
                  <ListItemIcon><img src={DeactivateDeviceSVG} alt="De-Activate Devices" height={iconHeight} /></ListItemIcon>
                  <ListItemText>De-Activate Devices</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/addadmin">
                  <ListItemIcon><GroupAddIcon /></ListItemIcon>
                  <ListItemText>Add Administrator Devices</ListItemText>
                </ListItem>
                <ListItem button component={Link} to="/removeadmin">
                  <ListItemIcon><PermIdentityIcon /></ListItemIcon>
                  <ListItemText>Remove Administrator Devices</ListItemText>
                </ListItem>
              </List>
           </div>
         </Drawer>
       </nav>
       <Route exact path="/" render={()=><HomePage classes={classes} devices={this.state.devices} status={this.state.status}
          search={event=>this.search(event)} valueChanged={this.valueChanged} administrators={this.state.administrators}
          maxrows={this.state.maxrows} previous={this.previous} next={this.next}
          previousActive={this.state.previousActive} nextActive={this.state.nextActive} allCheckboxChanged={this.allCheckboxChanged}
          allChecked={this.state.allChecked} itemCheck={this.itemCheck} loading={this.state.loading} />} />
       <Route exact path="/assign" render={()=> <DeviceForm classes={classes}
          aliasFieldName="Assigned Alias" showDeviceId={true} showAlias={true} devices={this.state.devices}
          requestType="Assign Aliases" />} />
       <Route exact path="/unassign" render={()=> <DeviceForm classes={classes}
           aliasFieldName="Assigned Alias" showDeviceId={false} showAlias={true} devices={this.state.devices}
           requestType="Un-assign Alias" />} />
       <Route exact path="/lock" render={()=> <DeviceForm classes={classes}
           aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices}
           requestType="Lock Devices" />} />
       <Route exact path="/unlock" render={()=> <DeviceForm classes={classes}
           aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices}
           requestType="Un-lock Devices" />} />
       <Route exact path="/deactivate" render={()=> <DeviceForm classes={classes}
           aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices}
           requestType="De-Activate Devices" />} />
       <Route exact path="/addadmin" render={()=> <DeviceForm classes={classes}
           aliasFieldName="Administrator Name" showDeviceId={true} showAlias={true} devices={this.state.devices}
           requestType="Add Administrator Devices" />} />
       <Route exact path="/removeadmin" render={()=> <DeviceForm classes={classes}
           aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices}
           requestType="Remove Administrator Devices" />} />
       <Snackbar
    			variant="error"
    			anchorOrigin={{
    				vertical: 'top',
    				horizontal: 'center',
    			}}
    			open={this.state.error}
    			autoHideDuration={6000}
    			onClose={this.handleSnackbarClose}
    			ContentProps={{
    				'aria-describedby': 'message-id',
    			}}
    			message={<span id="message-id">{this.state.errorMessage}</span>}
    			action={[
    				<IconButton
    					key="close"
    					aria-label="Close"
    					color="inherit"
    					className={classes.close}
    					onClick={this.handleSnackbarClose}>
    					<CloseIcon />
    				</IconButton>,
    			]}
    		/>
      </div>
    );} else {
      return <div>
        <Grid container spacing={8}>
          <Grid item sm={3} lg={4} />
          <Grid item xs={12} sm={6} lg={4}>
            <Card className={classes.card}>
              <CardContent>
                <Typography>TOTP Server Management Application</Typography>
                <TextField label="Device ID/Alias"
                  fullWidth={true}
                  value={this.state.deviceId}
                  className={classes.textField}
                  placeholder="123456789012345"
                  margin="normal"
                  required={true}
                  onChange={event => this.valueChanged(event,"deviceId")} />
                <TextField label="TOTP Code"
                  fullWidth={true}
                  value={this.state.otp}
                  className={classes.textField}
                  margin="normal"
                  required={true}
                  type="password"
                  onChange={event => this.valueChanged(event,"otp")} />
              </CardContent>
              <CardActions>
                <IconButton variant="contained" color="default"
                  onClick={event=> this.signin(event)} aria-label="Edit">
                  <ArrowForwardIosIcon /> Sign In
                </IconButton>
              </CardActions>
            </Card>
          </Grid>
        </Grid>
      </div>
    }
  }
}
App.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withRouter(withStyles(styles)(App));
